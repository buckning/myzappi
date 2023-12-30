package com.amcglynn.myzappi.api;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.UserId;

import java.util.HashMap;
import java.util.Optional;

public class SessionRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "session";
    private static final String SESSION_ID_COLUMN = "session-id";
    private static final String USER_ID_COLUMN = "amazon-user-id";
    private static final String TTL_COLUMN = "ttl";

    public SessionRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }

    public Optional<Session> read(SessionId sessionId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(SESSION_ID_COLUMN, new AttributeValue(sessionId.toString()));

        var result = dbClient.getItem(request);
        if (result.getItem() == null) {
            return Optional.empty();
        }

        return Optional.of(new Session(sessionId,
                UserId.from(result.getItem().get(USER_ID_COLUMN).getS()),
                Long.parseLong(result.getItem().get(TTL_COLUMN).getN())));
    }

    public void write(Session session) {
        var item = new HashMap<String, AttributeValue>();
        item.put(SESSION_ID_COLUMN, new AttributeValue(session.getSessionId().toString()));
        item.put(USER_ID_COLUMN, new AttributeValue(session.getUserId().toString()));
        item.put(TTL_COLUMN, new AttributeValue().withN(String.valueOf(session.getTtl())));

        var request = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);
        dbClient.putItem(request);
    }

    public void delete(Session session) {
        var deleteItem = new HashMap<String, AttributeValue>();
        deleteItem.put(SESSION_ID_COLUMN, new AttributeValue(session.getSessionId().toString()));
        dbClient.deleteItem(new DeleteItemRequest(TABLE_NAME, deleteItem));
    }
}
