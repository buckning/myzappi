package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;

import java.time.Instant;
import java.util.Map;

public class AutomationProcessorLockRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "automation-processor-lock";
    private static final String LOCK_ID_COLUMN = "lock-id";
    private static final String RUN_ID_COLUMN = "runId";
    private static final String EXPIRES_AT_COLUMN = "expiresAt";
    private static final String LOCK_ID = "automation-processor";

    public AutomationProcessorLockRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }

    public boolean acquire(String runId, Instant expiresAt) {
        try {
            dbClient.putItem(new PutItemRequest()
                    .withTableName(TABLE_NAME)
                    .addItemEntry(LOCK_ID_COLUMN, new AttributeValue(LOCK_ID))
                    .addItemEntry(RUN_ID_COLUMN, new AttributeValue(runId))
                    .addItemEntry(EXPIRES_AT_COLUMN, new AttributeValue().withN(String.valueOf(expiresAt.getEpochSecond())))
                    .withConditionExpression("attribute_not_exists(#lockId) OR #expiresAt < :now")
                    .withExpressionAttributeNames(Map.of(
                            "#lockId", LOCK_ID_COLUMN,
                            "#expiresAt", EXPIRES_AT_COLUMN))
                    .withExpressionAttributeValues(Map.of(
                            ":now", new AttributeValue().withN(String.valueOf(Instant.now().getEpochSecond())))));
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    public boolean refresh(String runId, Instant expiresAt) {
        try {
            dbClient.updateItem(new UpdateItemRequest()
                    .withTableName(TABLE_NAME)
                    .addKeyEntry(LOCK_ID_COLUMN, new AttributeValue(LOCK_ID))
                    .withUpdateExpression("SET #expiresAt = :expiresAt")
                    .withConditionExpression("#runId = :runId")
                    .withExpressionAttributeNames(Map.of(
                            "#runId", RUN_ID_COLUMN,
                            "#expiresAt", EXPIRES_AT_COLUMN))
                    .withExpressionAttributeValues(Map.of(
                            ":runId", new AttributeValue(runId),
                            ":expiresAt", new AttributeValue().withN(String.valueOf(expiresAt.getEpochSecond())))));
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    public void release(String runId) {
        try {
            dbClient.deleteItem(new DeleteItemRequest()
                    .withTableName(TABLE_NAME)
                    .addKeyEntry(LOCK_ID_COLUMN, new AttributeValue(LOCK_ID))
                    .withConditionExpression("#runId = :runId")
                    .withExpressionAttributeNames(Map.of("#runId", RUN_ID_COLUMN))
                    .withExpressionAttributeValues(Map.of(":runId", new AttributeValue(runId))));
        } catch (ConditionalCheckFailedException ignored) {
        }
    }
}
