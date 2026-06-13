package com.amcglynn.myzappi.core.dal;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.Map;

import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.numberValue;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;

public class AutomationProcessorLockRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "automation-processor-lock";
    private static final String LOCK_ID_COLUMN = "lock-id";
    private static final String RUN_ID_COLUMN = "runId";
    private static final String EXPIRES_AT_COLUMN = "expiresAt";
    private static final String LOCK_ID = "automation-processor";

    public AutomationProcessorLockRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
    }

    public boolean acquire(String runId, Instant expiresAt) {
        try {
            dbClient.putItem(PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(Map.of(
                            LOCK_ID_COLUMN, stringValue(LOCK_ID),
                            RUN_ID_COLUMN, stringValue(runId),
                            EXPIRES_AT_COLUMN, numberValue(expiresAt.getEpochSecond())))
                    .conditionExpression("attribute_not_exists(#lockId) OR #expiresAt < :now")
                    .expressionAttributeNames(Map.of(
                            "#lockId", LOCK_ID_COLUMN,
                            "#expiresAt", EXPIRES_AT_COLUMN))
                    .expressionAttributeValues(Map.of(
                            ":now", numberValue(Instant.now().getEpochSecond())))
                    .build());
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    public boolean refresh(String runId, Instant expiresAt) {
        try {
            dbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of(LOCK_ID_COLUMN, stringValue(LOCK_ID)))
                    .updateExpression("SET #expiresAt = :expiresAt")
                    .conditionExpression("#runId = :runId")
                    .expressionAttributeNames(Map.of(
                            "#runId", RUN_ID_COLUMN,
                            "#expiresAt", EXPIRES_AT_COLUMN))
                    .expressionAttributeValues(Map.of(
                            ":runId", stringValue(runId),
                            ":expiresAt", numberValue(expiresAt.getEpochSecond())))
                    .build());
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    public void release(String runId) {
        try {
            dbClient.deleteItem(DeleteItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of(LOCK_ID_COLUMN, stringValue(LOCK_ID)))
                    .conditionExpression("#runId = :runId")
                    .expressionAttributeNames(Map.of("#runId", RUN_ID_COLUMN))
                    .expressionAttributeValues(Map.of(":runId", stringValue(runId)))
                    .build());
        } catch (ConditionalCheckFailedException ignored) {
        }
    }
}
