package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

public class DeviceStateReconcileRequestsRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "device-state-reconcile-requests";
    private static final String DEVICE_STATE_CHANGE_KEY = "device-state-change-key";
    private static final String REQUEST_ID_COLUMN = "request-id";
    private static final String TTL_COLUMN = "ttl";
    private final ObjectMapper objectMapper;
    private final Supplier<Instant> instantSupplier;

    public DeviceStateReconcileRequestsRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.instantSupplier = Instant::now;
    }

    @SneakyThrows
    public Optional<String> read(UserId userId, SerialNumber serialNumber, String stateType) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(DEVICE_STATE_CHANGE_KEY, new AttributeValue(buildKey(userId, serialNumber, stateType)));

        var result = dbClient.getItem(request);
        if (result.getItem() == null) {
            return Optional.empty();
        }

        return Optional.of(result.getItem().get(REQUEST_ID_COLUMN).getS());
    }

    @SneakyThrows
    public void write(UserId userId, SerialNumber serialNumber, String stateType, String requestId) {
        var item = new HashMap<String, AttributeValue>();
        item.put(DEVICE_STATE_CHANGE_KEY, new AttributeValue(buildKey(userId, serialNumber, stateType)));

        item.put(REQUEST_ID_COLUMN, new AttributeValue(requestId));
        var ttl = instantSupplier.get().plus(Duration.ofSeconds(3600));
        item.put(TTL_COLUMN, new AttributeValue().withN(String.valueOf(ttl.getEpochSecond())));

        var request = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);
        dbClient.putItem(request);
    }

    private String buildKey(UserId userId, SerialNumber serialNumber, String stateType) {
        return userId.toString() + "#" + serialNumber.toString() + "#" + stateType;
    }
}
