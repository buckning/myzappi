package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.MyEnergiAccountCredentialsEncrypted;
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
class MyEnergiAccountCredentialsRepositoryTest {

    @Mock
    private DynamoDbClient mockDb;

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;
    @Captor
    private ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;

    private MyEnergiAccountCredentialsRepository credentialsRepository;
    private ByteBuffer encryptedEmailAddress;
    private ByteBuffer encryptedPassword;

    @BeforeEach
    void setUp() {
        encryptedEmailAddress = ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03});
        encryptedPassword = ByteBuffer.wrap(new byte[]{0x04, 0x05, 0x06});
        credentialsRepository = new MyEnergiAccountCredentialsRepository(mockDb);
    }

    @Test
    void testReadCredentialsForUserWhoDoesNotExistReturnsEmptyOptional() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
        var result = credentialsRepository.read("unknownuserid");
        assertThat(result).isEmpty();
    }

    @Test
    void testWriteCredentials() {
        var creds = new MyEnergiAccountCredentialsEncrypted("userid", encryptedEmailAddress, encryptedPassword);
        credentialsRepository.write(creds);
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().item()).hasSize(3);
        assertThat(putItemCaptor.getValue().tableName()).isEqualTo("myenergi-creds");
        assertThat(putItemCaptor.getValue().item().get("amazon-user-id").s()).isEqualTo("userid");
        assertThat(putItemCaptor.getValue().item().get("encrypted-email-address").b().asByteBuffer()).isEqualTo(encryptedEmailAddress);
        assertThat(putItemCaptor.getValue().item().get("encrypted-password").b().asByteBuffer()).isEqualTo(encryptedPassword);
    }

    @Test
    void testReadCredentials() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder()
                .item(Map.of(
                        "encrypted-email-address", binaryValue(encryptedEmailAddress),
                        "encrypted-password", binaryValue(encryptedPassword)))
                .build());
        var result = credentialsRepository.read("userid");
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("userid");
        assertThat(result.get().getEncryptedEmailAddress()).isEqualTo(encryptedEmailAddress);
        assertThat(result.get().getEncryptedPassword()).isEqualTo(encryptedPassword);
    }

    @Test
    void testDeleteSchedule() {
        credentialsRepository.delete("userId");
        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue()).isNotNull();
        assertThat(deleteItemCaptor.getValue().tableName()).isEqualTo("myenergi-creds");
        assertThat(deleteItemCaptor.getValue().key().get("amazon-user-id").s()).isEqualTo("userId");
    }
}
