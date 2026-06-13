package com.amcglynn.myzappi.api;

import com.amcglynn.myzappi.core.model.UserId;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.numberValue;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;

public class SessionRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "session";
    private static final String SESSION_ID_COLUMN = "session-id";
    private static final String USER_ID_COLUMN = "amazon-user-id";
    private static final String TTL_COLUMN = "ttl";

    public SessionRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
    }

    public Optional<Session> read(SessionId sessionId) {
        var request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(SESSION_ID_COLUMN, stringValue(sessionId.toString())))
                .build();

        var result = dbClient.getItem(request);
        if (!result.hasItem()) {
            return Optional.empty();
        }

        return Optional.of(new Session(sessionId,
                UserId.from(result.item().get(USER_ID_COLUMN).s()),
                Long.parseLong(result.item().get(TTL_COLUMN).n())));
    }

    public void write(Session session) {
        var item = new HashMap<String, AttributeValue>();
        item.put(SESSION_ID_COLUMN, stringValue(session.getSessionId().toString()));
        item.put(USER_ID_COLUMN, stringValue(session.getUserId().toString()));
        item.put(TTL_COLUMN, numberValue(session.getTtl()));

        var request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
        dbClient.putItem(request);
    }

    public void delete(Session session) {
        var deleteItem = new HashMap<String, AttributeValue>();
        deleteItem.put(SESSION_ID_COLUMN, stringValue(session.getSessionId().toString()));
        dbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(deleteItem)
                .build());
    }
}
