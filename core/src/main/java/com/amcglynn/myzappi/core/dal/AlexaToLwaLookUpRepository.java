package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.ZappiCredentials;

import java.util.HashMap;
import java.util.Optional;

public class AlexaToLwaLookUpRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "alexa-to-lwa-users-lookup";
    private static final String ALEXA_USER_ID_COLUMN = "alexa-user-id";
    private static final String LWA_USER_ID_COLUMN = "lwa-user-id";

    public AlexaToLwaLookUpRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }

    public Optional<String> getLwaUserId(String alexaUserId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(ALEXA_USER_ID_COLUMN, new AttributeValue(alexaUserId));

        var result = dbClient.getItem(request);
        if (result.getItem() == null) {
            return Optional.empty();
        }
        return Optional.of(result.getItem().get(LWA_USER_ID_COLUMN).getS());
    }

    public void write(String alexaUserId, String lwaUserId) {
        var item = new HashMap<String, AttributeValue>();
        item.put(ALEXA_USER_ID_COLUMN, new AttributeValue(alexaUserId));
        item.put(LWA_USER_ID_COLUMN, new AttributeValue(lwaUserId));

        var request = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);
        dbClient.putItem(request);
    }

    public void delete(String userId) {
        var deleteItem = new HashMap<String, AttributeValue>();
        deleteItem.put(ALEXA_USER_ID_COLUMN, new AttributeValue(userId));
        dbClient.deleteItem(new DeleteItemRequest(TABLE_NAME, deleteItem));
    }
}
