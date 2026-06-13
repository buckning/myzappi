package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.AlexaToLwaUserDetails;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Optional;

import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;

public class AlexaToLwaLookUpRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "alexa-to-lwa-users-lookup";
    private static final String ALEXA_USER_ID_COLUMN = "alexa-user-id";
    private static final String LWA_USER_ID_COLUMN = "lwa-user-id";
    private static final String ZONE_ID_COLUMN = "zone-id";

    public AlexaToLwaLookUpRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
    }

    public Optional<AlexaToLwaUserDetails> read(String alexaUserId) {
        var request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(java.util.Map.of(ALEXA_USER_ID_COLUMN, stringValue(alexaUserId)))
                .build();

        var result = dbClient.getItem(request);
        if (!result.hasItem()) {
            return Optional.empty();
        }
        return Optional.of(new AlexaToLwaUserDetails(result.item().get(ALEXA_USER_ID_COLUMN).s(),
                result.item().get(LWA_USER_ID_COLUMN).s(),
                result.item().get(ZONE_ID_COLUMN).s()));
    }

    public void write(String alexaUserId, String lwaUserId, String zoneId) {
        var item = new HashMap<String, AttributeValue>();
        item.put(ALEXA_USER_ID_COLUMN, stringValue(alexaUserId));
        item.put(LWA_USER_ID_COLUMN, stringValue(lwaUserId));
        item.put(ZONE_ID_COLUMN, stringValue(zoneId));

        var request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
        dbClient.putItem(request);
    }

    public void delete(String userId) {
        var deleteItem = new HashMap<String, AttributeValue>();
        deleteItem.put(ALEXA_USER_ID_COLUMN, stringValue(userId));
        dbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(deleteItem)
                .build());
    }
}
