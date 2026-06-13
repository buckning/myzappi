package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.MyEnergiDeployment;
import com.amcglynn.myzappi.core.model.SerialNumber;
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
public class CredentialsRepository {

    private final DynamoDbClient dbClient;
    private static final String TABLE_NAME = "zappi-login-creds";
    private static final String USER_ID_COLUMN = "amazon-user-id";
    private static final String SERIAL_NUMBER_COLUMN = "serial-number";
    private static final String ENCRYPTED_API_KEY_COLUMN = "encrypted-api-key";

    public CredentialsRepository(DynamoDbClient dbClient) {
        this.dbClient = dbClient;
    }

    public Optional<MyEnergiDeployment> read(String userId) {
        var request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(java.util.Map.of(USER_ID_COLUMN, stringValue(userId)))
                .build();

        var result = dbClient.getItem(request);
        if (!result.hasItem()) {
            return Optional.empty();
        }
        var serialNumber = result.item().get(SERIAL_NUMBER_COLUMN).s();
        var encrypted = byteBufferValue(result.item().get(ENCRYPTED_API_KEY_COLUMN));
        return Optional.of(new MyEnergiDeployment(userId,
                SerialNumber.from(serialNumber),
                encrypted));
    }

    public void write(MyEnergiDeployment creds) {
        var item = new HashMap<String, AttributeValue>();
        item.put(USER_ID_COLUMN, stringValue(creds.getUserId()));
        item.put(SERIAL_NUMBER_COLUMN, stringValue(creds.getSerialNumber().toString()));
        item.put(ENCRYPTED_API_KEY_COLUMN, binaryValue(creds.getEncryptedApiKey()));

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
