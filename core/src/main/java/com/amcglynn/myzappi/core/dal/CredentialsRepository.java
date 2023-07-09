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

public class CredentialsRepository {

    private final AmazonDynamoDB dbClient;
    private static final String TABLE_NAME = "zappi-login-creds";
    private static final String USER_ID_COLUMN = "amazon-user-id";
    private static final String ZAPPI_SERIAL_NUMBER_COLUMN = "zappi-serial-number";
    private static final String SERIAL_NUMBER_COLUMN = "serial-number";
    private static final String ENCRYPTED_API_KEY_COLUMN = "encrypted-api-key";

    public CredentialsRepository(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }

    public Optional<ZappiCredentials> read(String userId) {
        var request = new GetItemRequest()
                .withTableName(TABLE_NAME)
                .addKeyEntry(USER_ID_COLUMN, new AttributeValue(userId));

        var result = dbClient.getItem(request);
        if (result.getItem() == null) {
            return Optional.empty();
        }
        var zappiSerialNumber = result.getItem().get(ZAPPI_SERIAL_NUMBER_COLUMN).getS();
        var serialNumber = result.getItem().get(SERIAL_NUMBER_COLUMN).getS();
        var encrypted = result.getItem().get(ENCRYPTED_API_KEY_COLUMN).getB();
        return Optional.of(new ZappiCredentials(userId,
                SerialNumber.from(zappiSerialNumber),
                SerialNumber.from(serialNumber),
                encrypted));
    }

    public void write(ZappiCredentials creds) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, new AttributeValue(creds.getUserId()));
        item.put(SERIAL_NUMBER_COLUMN, new AttributeValue(creds.getSerialNumber().toString()));
        item.put(ZAPPI_SERIAL_NUMBER_COLUMN, new AttributeValue(creds.getZappiSerialNumber().toString()));
        item.put(ENCRYPTED_API_KEY_COLUMN, new AttributeValue().withB(creds.getEncryptedApiKey()));

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
