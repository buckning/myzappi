package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.AutomationStateEntry;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;

public class AutomationStateRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "automation-state";
    private static final String USER_ID_COLUMN = "user-id";
    private static final String STATES_COLUMN = "states";

    private final ObjectMapper objectMapper;

    public AutomationStateRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @SneakyThrows
    public Map<String, AutomationStateEntry> read(UserId userId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId.toString()));

        var result = dbClient.getItem(request);
        if (result.getItem() == null || result.getItem().get(STATES_COLUMN) == null) {
            return Map.of();
        }

        return objectMapper.readValue(result.getItem().get(STATES_COLUMN).getS(), new TypeReference<>() {
        });
    }

    @SneakyThrows
    public void write(UserId userId, Map<String, AutomationStateEntry> states) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, new AttributeValue(userId.toString()));
        item.put(STATES_COLUMN, new AttributeValue(objectMapper.writeValueAsString(states)));

        dbClient.putItem(new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item));
    }

    public void delete(UserId userId) {
        dbClient.deleteItem(new DeleteItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId.toString())));
    }
}
