package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;

public class DevicesRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "devices";
    private static final String USER_ID_COLUMN = "user-id";
    private static final String DEVICES_COLUMN = "devices";
    private final ObjectMapper objectMapper;

    public DevicesRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @SneakyThrows
    public List<MyEnergiDevice> read(UserId userId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId.toString()));

        var result = dbClient.getItem(request);
        if (result.getItem() == null) {
            return List.of();
        }

        var bodyText = result.getItem().get(DEVICES_COLUMN).getS();
        return objectMapper.readValue(bodyText, new TypeReference<>() {
        });
    }

    @SneakyThrows
    public void write(UserId userId, List<MyEnergiDevice> devices) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, new AttributeValue(userId.toString()));

        var devicesString = objectMapper.writeValueAsString(devices);
        item.put(DEVICES_COLUMN, new AttributeValue(devicesString));

        var request = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);
        dbClient.putItem(request);
    }

    public void delete(UserId userId) {
        var request = new DeleteItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId.toString()));
        dbClient.deleteItem(request);
    }
}
