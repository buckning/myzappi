package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.MyEnergiDeployment;
import com.amcglynn.myzappi.core.model.SerialNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.binaryValue;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialsRepositoryTest {

    @Mock
    private DynamoDbClient mockDb;

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;
    @Captor
    private ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;

    private CredentialsRepository credentialsRepository;
    private ByteBuffer encryptedApiKey;

    @BeforeEach
    void setUp() {
        encryptedApiKey = ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03});
        credentialsRepository = new CredentialsRepository(mockDb);
    }

    @Test
    void testReadCredentialsForUserWhoDoesNotExistReturnsEmptyOptional() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
        var result = credentialsRepository.read("unknownuserid");
        assertThat(result).isEmpty();
    }

    @Test
    void testWriteCredentialsWithEddi() {
        var creds = new MyEnergiDeployment("userid", SerialNumber.from("12345678"), encryptedApiKey);
        credentialsRepository.write(creds);
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().item()).hasSize(3);
        assertThat(putItemCaptor.getValue().tableName()).isEqualTo("zappi-login-creds");
        assertThat(putItemCaptor.getValue().item().get("amazon-user-id").s()).isEqualTo("userid");
        assertThat(putItemCaptor.getValue().item().get("serial-number").s()).isEqualTo("12345678");
        assertThat(putItemCaptor.getValue().item().get("encrypted-api-key").b().asByteBuffer()).isEqualTo(encryptedApiKey);
    }


    @Test
    void testReadCredentialsForUserWhoHasSuccessfullyLoggedInAndWhoHasAnEddi() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder()
                .item(Map.of("serial-number", stringValue("12345678"),
                        "encrypted-api-key", binaryValue(encryptedApiKey)))
                .build());
        var result = credentialsRepository.read("userid");
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("userid");
        assertThat(result.get().getSerialNumber()).isEqualTo(SerialNumber.from("12345678"));
        assertThat(result.get().getSerialNumber()).hasSameHashCodeAs(SerialNumber.from("12345678"));
        assertThat(result.get().getEncryptedApiKey()).isEqualTo(encryptedApiKey);
    }

    @Test
    void testDeleteCredentials() {
        credentialsRepository.delete("userid");
        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue()).isNotNull();
        assertThat(deleteItemCaptor.getValue().tableName()).isEqualTo("zappi-login-creds");
        assertThat(deleteItemCaptor.getValue().key().get("amazon-user-id").s()).isEqualTo("userid");
    }
}
