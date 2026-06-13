package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.MyEnergiAccountCredentialsEncrypted;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Optional;

import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.binaryValue;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.byteBufferValue;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;

@Slf4j
public class MyEnergiAccountCredentialsRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "myenergi-creds";
    private static final String USER_ID_COLUMN = "amazon-user-id";
    private static final String ENCRYPTED_EMAIL_COLUMN = "encrypted-email-address";
    private static final String ENCRYPTED_PASSWORD_COLUMN = "encrypted-password";

    public MyEnergiAccountCredentialsRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
    }

    public Optional<MyEnergiAccountCredentialsEncrypted> read(String userId) {
        var request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(java.util.Map.of(USER_ID_COLUMN, stringValue(userId)))
                .build();

        var result = dbClient.getItem(request);
        if (!result.hasItem()) {
            return Optional.empty();
        }

        return Optional.of(new MyEnergiAccountCredentialsEncrypted(userId,
                byteBufferValue(result.item().get(ENCRYPTED_EMAIL_COLUMN)),
                byteBufferValue(result.item().get(ENCRYPTED_PASSWORD_COLUMN))));
    }

    public void write(MyEnergiAccountCredentialsEncrypted creds) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, stringValue(creds.getUserId()));
        item.put(ENCRYPTED_EMAIL_COLUMN, binaryValue(creds.getEncryptedEmailAddress()));
        item.put(ENCRYPTED_PASSWORD_COLUMN, binaryValue(creds.getEncryptedPassword()));

        var request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
        dbClient.putItem(request);
    }

    public void delete(String userId) {
        var deleteItem = new HashMap<String, AttributeValue>();
        deleteItem.put(USER_ID_COLUMN, stringValue(userId));
        dbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(deleteItem)
                .build());
    }
}
