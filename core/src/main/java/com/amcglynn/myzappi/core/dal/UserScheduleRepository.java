package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.SneakyThrows;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;

public class UserScheduleRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "schedule";
    private static final String USER_ID_COLUMN = "user-id";
    private static final String SCHEDULES_COLUMN = "schedules";

    private ObjectMapper objectMapper;

    public UserScheduleRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @SneakyThrows
    public List<Schedule> read(UserId userId) {
        var request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(USER_ID_COLUMN, stringValue(userId.toString())))
                .build();

        var result = dbClient.getItem(request);
        if (!result.hasItem()) {
            return List.of();
        }

        var bodyText = result.item().get(SCHEDULES_COLUMN).s();
        return objectMapper.readValue(bodyText, new TypeReference<>() {
        });
    }

    @SneakyThrows
    public void write(UserId userId, List<Schedule> schedules) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, stringValue(userId.toString()));
        var schedulesString = objectMapper.writeValueAsString(schedules);
        item.put(SCHEDULES_COLUMN, stringValue(schedulesString));

        var request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
        dbClient.putItem(request);
    }

    @SneakyThrows
    public void update(UserId userId, List<Schedule> schedules) {
        dbClient.updateItem(UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(USER_ID_COLUMN, stringValue(userId.toString())))
                .updateExpression("SET #schedules = :schedules")
                .expressionAttributeNames(Map.of("#schedules", SCHEDULES_COLUMN))
                .expressionAttributeValues(Map.of(":schedules", stringValue(objectMapper.writeValueAsString(schedules))))
                .build());
    }
}
