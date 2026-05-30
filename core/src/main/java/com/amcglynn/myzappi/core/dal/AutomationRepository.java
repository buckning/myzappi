package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.AutomationScanPage;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AutomationRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "automation";
    private static final String USER_ID_COLUMN = "user-id";
    private static final String AUTOMATIONS_COLUMN = "automations";

    private final ObjectMapper objectMapper;

    public AutomationRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @SneakyThrows
    public List<Automation> read(UserId userId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId.toString()));

        var result = dbClient.getItem(request);
        if (result.getItem() == null || result.getItem().get(AUTOMATIONS_COLUMN) == null) {
            return List.of();
        }

        return objectMapper.readValue(result.getItem().get(AUTOMATIONS_COLUMN).getS(), new TypeReference<>() {
        });
    }

    @SneakyThrows
    public void write(UserId userId, List<Automation> automations) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, new AttributeValue(userId.toString()));
        item.put(AUTOMATIONS_COLUMN, new AttributeValue(objectMapper.writeValueAsString(automations)));

        dbClient.putItem(new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item));
    }

    public void delete(UserId userId) {
        dbClient.deleteItem(new DeleteItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId.toString())));
    }

    public AutomationScanPage scan(Map<String, AttributeValue> exclusiveStartKey, int limit) {
        var request = new ScanRequest()
                .withTableName(TABLE_NAME)
                .withLimit(limit);
        if (exclusiveStartKey != null && !exclusiveStartKey.isEmpty()) {
            request.withExclusiveStartKey(exclusiveStartKey);
        }

        var result = dbClient.scan(request);
        var userAutomations = new ArrayList<AutomationScanPage.UserAutomations>();
        for (var item : result.getItems()) {
            parseScanItem(item).ifPresent(userAutomations::add);
        }

        return AutomationScanPage.builder()
                .userAutomations(userAutomations)
                .lastEvaluatedKey(result.getLastEvaluatedKey())
                .build();
    }

    private java.util.Optional<AutomationScanPage.UserAutomations> parseScanItem(Map<String, AttributeValue> item) {
        try {
            if (!item.containsKey(USER_ID_COLUMN) || !item.containsKey(AUTOMATIONS_COLUMN)) {
                return java.util.Optional.empty();
            }
            var automations = objectMapper.readValue(item.get(AUTOMATIONS_COLUMN).getS(),
                    new TypeReference<List<Automation>>() {
                    });
            return java.util.Optional.of(AutomationScanPage.UserAutomations.builder()
                    .userId(UserId.from(item.get(USER_ID_COLUMN).getS()))
                    .automations(automations)
                    .build());
        } catch (Exception e) {
            log.warn("Skipping malformed automation row for user {}", item.get(USER_ID_COLUMN), e);
            return java.util.Optional.empty();
        }
    }
}
