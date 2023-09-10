package com.amcglynn.myzappi.api;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;

import java.util.HashMap;
import java.util.Optional;

public class SessionRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "session";
    private static final String SESSION_ID_COLUMN = "session-id";
    private static final String USER_ID_COLUMN = "amazon-user-id";
    private static final String ENCRYPTED_TOKEN_COLUMN = "token";
    private static final String TTL_COLUMN = "ttl";

    public SessionRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }

    public Optional<Session> read(String sessionId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(SESSION_ID_COLUMN, new AttributeValue(sessionId));

        var result = dbClient.getItem(request);
        if (result.getItem() == null) {
            return Optional.empty();
        }

        return Optional.of(new Session(sessionId,
                result.getItem().get(USER_ID_COLUMN).getS(),
                result.getItem().get(ENCRYPTED_TOKEN_COLUMN).getB(),
                Long.parseLong(result.getItem().get(TTL_COLUMN).getN())));
    }

    public void write(Session session) {
        var item = new HashMap<String, AttributeValue>();
        item.put(SESSION_ID_COLUMN, new AttributeValue(session.getSessionId()));
        item.put(USER_ID_COLUMN, new AttributeValue(session.getUserId()));
        item.put(ENCRYPTED_TOKEN_COLUMN, new AttributeValue().withB(session.getEncryptedToken()));
        item.put(TTL_COLUMN, new AttributeValue().withN(String.valueOf(session.getTtl())));

        var request = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);
        dbClient.putItem(request);
    }

    public void delete(Session session) {
        var deleteItem = new HashMap<String, AttributeValue>();
        deleteItem.put(SESSION_ID_COLUMN, new AttributeValue(session.getSessionId()));
        dbClient.deleteItem(new DeleteItemRequest(TABLE_NAME, deleteItem));
    }
}
