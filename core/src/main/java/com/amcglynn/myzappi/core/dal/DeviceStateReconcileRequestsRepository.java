package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.numberValue;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;

public class DeviceStateReconcileRequestsRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "device-state-reconcile-requests";
    private static final String DEVICE_STATE_CHANGE_KEY = "device-state-change-key";
    private static final String REQUEST_ID_COLUMN = "request-id";
    private static final String TTL_COLUMN = "ttl";
    private final ObjectMapper objectMapper;
    private final Supplier<Instant> instantSupplier;

    public DeviceStateReconcileRequestsRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.instantSupplier = Instant::now;
    }

    @SneakyThrows
    public Optional<String> read(UserId userId, SerialNumber serialNumber, String stateType) {
        var request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(DEVICE_STATE_CHANGE_KEY, stringValue(buildKey(userId, serialNumber, stateType))))
                .build();

        var result = dbClient.getItem(request);
        if (!result.hasItem()) {
            return Optional.empty();
        }

        return Optional.of(result.item().get(REQUEST_ID_COLUMN).s());
    }

    @SneakyThrows
    public void write(UserId userId, SerialNumber serialNumber, String stateType, String requestId) {
        var item = new HashMap<String, AttributeValue>();
        item.put(DEVICE_STATE_CHANGE_KEY, stringValue(buildKey(userId, serialNumber, stateType)));

        item.put(REQUEST_ID_COLUMN, stringValue(requestId));
        var ttl = instantSupplier.get().plus(Duration.ofSeconds(3600));
        item.put(TTL_COLUMN, numberValue(ttl.getEpochSecond()));

        var request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
        dbClient.putItem(request);
    }

    private String buildKey(UserId userId, SerialNumber serialNumber, String stateType) {
        return userId.toString() + "#" + serialNumber.toString() + "#" + stateType;
    }
}
