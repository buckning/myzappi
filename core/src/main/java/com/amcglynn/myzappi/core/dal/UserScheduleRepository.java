package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;

public class UserScheduleRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "schedule";
    private static final String USER_ID_COLUMN = "user-id";
    private static final String SCHEDULES_COLUMN = "schedules";

    private ObjectMapper objectMapper;

    public UserScheduleRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @SneakyThrows
    public List<Schedule> read(UserId userId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId.toString()));

        var result = dbClient.getItem(request);
        if (result.getItem() == null) {
            return List.of();
        }

        var bodyText = result.getItem().get(SCHEDULES_COLUMN).getS();
        return objectMapper.readValue(bodyText, new TypeReference<>() {
        });
    }

    @SneakyThrows
    public void write(UserId userId, List<Schedule> schedules) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, new AttributeValue(userId.toString()));
        var schedulesString = objectMapper.writeValueAsString(schedules);
        item.put(SCHEDULES_COLUMN, new AttributeValue(schedulesString));

        var request = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);
        dbClient.putItem(request);
    }

    @SneakyThrows
    public void update(UserId userId, List<Schedule> schedules) {
        dbClient.updateItem(new UpdateItemRequest().withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId.toString()))
                .addAttributeUpdatesEntry(SCHEDULES_COLUMN,
                        new AttributeValueUpdate().withValue(new AttributeValue(objectMapper.writeValueAsString(schedules)))));
    }
}
