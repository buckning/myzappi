package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.AutomationScanPage;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;

@Slf4j
public class AutomationRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "automation";
    private static final String USER_ID_COLUMN = "user-id";
    private static final String AUTOMATIONS_COLUMN = "automations";

    private final ObjectMapper objectMapper;

    public AutomationRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @SneakyThrows
    public List<Automation> read(UserId userId) {
        var request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(USER_ID_COLUMN, stringValue(userId.toString())))
                .build();

        var result = dbClient.getItem(request);
        if (!result.hasItem() || result.item().get(AUTOMATIONS_COLUMN) == null) {
            return List.of();
        }

        return objectMapper.readValue(result.item().get(AUTOMATIONS_COLUMN).s(), new TypeReference<>() {
        });
    }

    @SneakyThrows
    public void write(UserId userId, List<Automation> automations) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, stringValue(userId.toString()));
        item.put(AUTOMATIONS_COLUMN, stringValue(objectMapper.writeValueAsString(automations)));

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

    public AutomationScanPage scan(Map<String, String> exclusiveStartKey, int limit) {
        var request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .limit(limit);
        if (exclusiveStartKey != null && !exclusiveStartKey.isEmpty()) {
            request.exclusiveStartKey(toDynamoDbKey(exclusiveStartKey));
        }

        var result = dbClient.scan(request.build());
        var userAutomations = new ArrayList<AutomationScanPage.UserAutomations>();
        for (var item : result.items()) {
            parseScanItem(item).ifPresent(userAutomations::add);
        }

        return AutomationScanPage.builder()
                .userAutomations(userAutomations)
                .lastEvaluatedKey(toSerializableKey(result.lastEvaluatedKey()))
                .build();
    }

    private java.util.Optional<AutomationScanPage.UserAutomations> parseScanItem(Map<String, AttributeValue> item) {
        try {
            if (!item.containsKey(USER_ID_COLUMN) || !item.containsKey(AUTOMATIONS_COLUMN)) {
                return java.util.Optional.empty();
            }
            var automations = objectMapper.readValue(item.get(AUTOMATIONS_COLUMN).s(),
                    new TypeReference<List<Automation>>() {
                    });
            return java.util.Optional.of(AutomationScanPage.UserAutomations.builder()
                    .userId(UserId.from(item.get(USER_ID_COLUMN).s()))
                    .automations(automations)
                    .build());
        } catch (Exception e) {
            log.warn("Skipping malformed automation row for user {}", item.get(USER_ID_COLUMN), e);
            return java.util.Optional.empty();
        }
    }

    private Map<String, AttributeValue> toDynamoDbKey(Map<String, String> exclusiveStartKey) {
        var result = new HashMap<String, AttributeValue>();
        exclusiveStartKey.forEach((key, value) -> result.put(key, stringValue(value)));
        return result;
    }

    private Map<String, String> toSerializableKey(Map<String, AttributeValue> lastEvaluatedKey) {
        if (lastEvaluatedKey == null || lastEvaluatedKey.isEmpty()) {
            return null;
        }
        var result = new HashMap<String, String>();
        lastEvaluatedKey.forEach((key, value) -> result.put(key, value.s()));
        return result;
    }
}
