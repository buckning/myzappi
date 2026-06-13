package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.AutomationStateEntry;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.SneakyThrows;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;

public class AutomationStateRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "automation-state";
    private static final String USER_ID_COLUMN = "user-id";
    private static final String STATES_COLUMN = "states";

    private final ObjectMapper objectMapper;

    public AutomationStateRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @SneakyThrows
    public Map<String, AutomationStateEntry> read(UserId userId) {
        var request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(USER_ID_COLUMN, stringValue(userId.toString())))
                .build();

        var result = dbClient.getItem(request);
        if (!result.hasItem() || result.item().get(STATES_COLUMN) == null) {
            return Map.of();
        }

        return objectMapper.readValue(result.item().get(STATES_COLUMN).s(), new TypeReference<>() {
        });
    }

    @SneakyThrows
    public void write(UserId userId, Map<String, AutomationStateEntry> states) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, stringValue(userId.toString()));
        item.put(STATES_COLUMN, stringValue(objectMapper.writeValueAsString(states)));

        dbClient.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build());
    }

    public void delete(UserId userId) {
        dbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(USER_ID_COLUMN, stringValue(userId.toString())))
                .build());
    }
}
