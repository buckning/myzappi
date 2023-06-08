package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.LoginCodeEntry;
import com.amcglynn.myzappi.core.model.ZappiCredentials;
import com.amcglynn.myzappi.core.service.LoginCode;

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
        codeItem.put(CODE_COLUMN, new AttributeValue(creds.getCode().toString()));
        codeItem.put(USER_ID_COLUMN, new AttributeValue(creds.getUserId()));
        codeItem.put(CREATED_COLUMN, new AttributeValue().withN(String.valueOf(Instant.now().toEpochMilli())));
        var putRequest = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(codeItem);
        dbClient.putItem(putRequest);
    }

    public Optional<LoginCodeEntry> read(LoginCode code) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(CODE_COLUMN, new AttributeValue(code.toString()));
        return Optional.ofNullable(dbClient.getItem(request).getItem())
                .map(item -> new LoginCodeEntry(LoginCode.from(item.get(CODE_COLUMN).getS()),
                        item.get(USER_ID_COLUMN).getS(),
                        Instant.ofEpochMilli(Long.parseLong(item.get(CREATED_COLUMN).getN()))));
    }

    public void delete(LoginCode code) {
        var deleteItem = new HashMap<String, AttributeValue>();
        deleteItem.put(CODE_COLUMN, new AttributeValue(code.toString()));
        dbClient.deleteItem(new DeleteItemRequest(TABLE_NAME, deleteItem));
    }
}
