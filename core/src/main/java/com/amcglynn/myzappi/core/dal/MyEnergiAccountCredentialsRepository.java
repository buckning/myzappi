package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.MyEnergiAccountCredentialsEncrypted;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Optional;

@Slf4j
public class MyEnergiAccountCredentialsRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "myenergi-creds";
    private static final String USER_ID_COLUMN = "amazon-user-id";
    private static final String ENCRYPTED_EMAIL_COLUMN = "encrypted-email-address";
    private static final String ENCRYPTED_PASSWORD_COLUMN = "encrypted-password";

    public MyEnergiAccountCredentialsRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }

    public Optional<MyEnergiAccountCredentialsEncrypted> read(String userId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId));

        var result = dbClient.getItem(request);
        if (result.getItem() == null) {
            return Optional.empty();
        }

        return Optional.of(new MyEnergiAccountCredentialsEncrypted(userId,
                result.getItem().get(ENCRYPTED_EMAIL_COLUMN).getB(),
                result.getItem().get(ENCRYPTED_PASSWORD_COLUMN).getB()));
    }

    public void write(MyEnergiAccountCredentialsEncrypted creds) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, new AttributeValue(creds.getUserId()));
        item.put(ENCRYPTED_EMAIL_COLUMN, new AttributeValue().withB(creds.getEncryptedEmailAddress()));
        item.put(ENCRYPTED_PASSWORD_COLUMN, new AttributeValue().withB(creds.getEncryptedPassword()));

        var request = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);
        dbClient.putItem(request);
    }

    public void delete(String userId) {
        var deleteItem = new HashMap<String, AttributeValue>();
        deleteItem.put(USER_ID_COLUMN, new AttributeValue(userId));
        dbClient.deleteItem(new DeleteItemRequest(TABLE_NAME, deleteItem));
    }
}
