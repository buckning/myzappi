package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.ScheduleDetails;
import com.amcglynn.myzappi.core.model.UserId;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Optional;

public class ScheduleDetailsRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "schedule-details";
    private static final String SCHEDULE_ID_COLUMN = "schedule-id";
    private static final String LWA_USER_ID_COLUMN = "lwa-user-id";

    public ScheduleDetailsRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }

    @SneakyThrows
    public Optional<ScheduleDetails> read(String scheduleId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(SCHEDULE_ID_COLUMN, new AttributeValue(scheduleId));

        var result = dbClient.getItem(request);
        if (result.getItem() == null) {
            return Optional.empty();
        }

        var lwaUserId = result.getItem().get(LWA_USER_ID_COLUMN).getS();
        return Optional.of(new ScheduleDetails(scheduleId, UserId.from(lwaUserId)));
    }

    @SneakyThrows
    public void write(String scheduleId, UserId userId) {
        var item = new HashMap<String, AttributeValue>();
        item.put(SCHEDULE_ID_COLUMN, new AttributeValue(scheduleId));
        item.put(LWA_USER_ID_COLUMN, new AttributeValue(userId.toString()));

        var request = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);
        dbClient.putItem(request);
    }

    public void delete(String scheduleId) {
        var deleteItem = new HashMap<String, AttributeValue>();
        deleteItem.put(SCHEDULE_ID_COLUMN, new AttributeValue(scheduleId));
        dbClient.deleteItem(new DeleteItemRequest(TABLE_NAME, deleteItem));
    }
}
