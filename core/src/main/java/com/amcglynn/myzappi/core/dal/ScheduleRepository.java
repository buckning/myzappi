package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.Schedule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;

public class ScheduleRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "schedule";
    private static final String USER_ID_COLUMN = "user-id";
    private static final String SCHEDULES_COLUMN = "schedules";

    public ScheduleRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }

    @SneakyThrows
    public List<Schedule> read(String userId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId));

        var result = dbClient.getItem(request);
        if (result.getItem() == null) {
            return List.of();
        }

        var bodyText = result.getItem().get(SCHEDULES_COLUMN).getS();
        return new ObjectMapper().readValue(bodyText, new TypeReference<>() {
        });
    }

    @SneakyThrows
    public void write(String userId, List<Schedule> schedules) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, new AttributeValue(userId));
        var schedulesString = new ObjectMapper().writeValueAsString(schedules);
        item.put(SCHEDULES_COLUMN, new AttributeValue(schedulesString));

        var request = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);
        dbClient.putItem(request);
    }
}
