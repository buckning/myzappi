package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.ScheduleDetails;
import com.amcglynn.myzappi.core.model.UserId;
import lombok.SneakyThrows;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Optional;

import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;

public class ScheduleDetailsRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "schedule-details";
    private static final String SCHEDULE_ID_COLUMN = "schedule-id";
    private static final String LWA_USER_ID_COLUMN = "lwa-user-id";

    public ScheduleDetailsRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @SneakyThrows
    public Optional<ScheduleDetails> read(String scheduleId) {
        var request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(java.util.Map.of(SCHEDULE_ID_COLUMN, stringValue(scheduleId)))
                .build();

        var result = dbClient.getItem(request);
        if (!result.hasItem()) {
            return Optional.empty();
        }

        var lwaUserId = result.item().get(LWA_USER_ID_COLUMN).s();
        return Optional.of(new ScheduleDetails(scheduleId, UserId.from(lwaUserId)));
    }

    @SneakyThrows
    public void write(String scheduleId, UserId userId) {
        var item = new HashMap<String, AttributeValue>();
        item.put(SCHEDULE_ID_COLUMN, stringValue(scheduleId));
        item.put(LWA_USER_ID_COLUMN, stringValue(userId.toString()));

        var request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
        dbClient.putItem(request);
    }

    public void delete(String scheduleId) {
        var deleteItem = new HashMap<String, AttributeValue>();
        deleteItem.put(SCHEDULE_ID_COLUMN, stringValue(scheduleId));
        dbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(deleteItem)
                .build());
    }
}
