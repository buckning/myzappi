package com.amcglynn.myzappi.core;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;

public class LoginCodeRepository {
    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "zappi-login-code";
    private static final String CODE_COLUMN = "code";
    private static final String USER_ID_COLUMN = "amazon-user-id";
    private static final String CREATED_COLUMN = "created";

    public LoginCodeRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }

    public void write(ZappiCredentials creds) {
        var codeItem = new HashMap<String, AttributeValue>();
        codeItem.put(CODE_COLUMN, new AttributeValue(creds.getCode()));
        codeItem.put(USER_ID_COLUMN, new AttributeValue(creds.getUserId()));
        codeItem.put(CREATED_COLUMN, new AttributeValue().withN(String.valueOf(Instant.now().toEpochMilli())));
        var putRequest = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(codeItem);
        dbClient.putItem(putRequest);
    }

    public Optional<LoginCodeEntry> read(String code) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(CODE_COLUMN, new AttributeValue(code));
        return Optional.ofNullable(dbClient.getItem(request).getItem())
                .map(item -> new LoginCodeEntry(item.get(CODE_COLUMN).getS(),
                        item.get(USER_ID_COLUMN).getS(),
                        Instant.ofEpochMilli(Long.parseLong(item.get(CREATED_COLUMN).getN()))));
    }

    public void delete(String code) {
        var deleteItem = new HashMap<String, AttributeValue>();
        deleteItem.put(CODE_COLUMN, new AttributeValue(code));
        dbClient.deleteItem(new DeleteItemRequest(TABLE_NAME, deleteItem));
    }
}
