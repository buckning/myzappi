package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.List;

import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;

public class DevicesRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "devices";
    private static final String USER_ID_COLUMN = "user-id";
    private static final String DEVICES_COLUMN = "devices";
    private final ObjectMapper objectMapper;

    public DevicesRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @SneakyThrows
    public List<MyEnergiDevice> read(UserId userId) {
        var request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(java.util.Map.of(USER_ID_COLUMN, stringValue(userId.toString())))
                .build();

        var result = dbClient.getItem(request);
        if (!result.hasItem()) {
            return List.of();
        }

        var bodyText = result.item().get(DEVICES_COLUMN).s();
        return objectMapper.readValue(bodyText, new TypeReference<>() {
        });
    }

    @SneakyThrows
    public void write(UserId userId, List<MyEnergiDevice> devices) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, stringValue(userId.toString()));

        var devicesString = objectMapper.writeValueAsString(devices);
        item.put(DEVICES_COLUMN, stringValue(devicesString));

        var request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
        dbClient.putItem(request);
    }

    public void delete(UserId userId) {
        var request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(java.util.Map.of(USER_ID_COLUMN, stringValue(userId.toString())))
                .build();
        dbClient.deleteItem(request);
    }
}
