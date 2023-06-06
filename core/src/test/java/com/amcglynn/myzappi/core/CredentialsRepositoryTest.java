package com.amcglynn.myzappi.core;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialsRepositoryTest {

    @Mock
    private AmazonDynamoDB mockDb;
    @Mock
    private GetItemResult mockGetResult;

    @Captor
    ArgumentCaptor<PutItemRequest> putItemCaptor;
    @Captor
    ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;

    private CredentialsRepository credentialsRepository;
    private ByteBuffer encryptedApiKey;

    @BeforeEach
    void setUp() {
        encryptedApiKey = ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03 });
        credentialsRepository = new CredentialsRepository(mockDb);
    }

    @Test
    void testReadCredentialsForUserWhoDoesNotExistReturnsEmptyOptional() {
        when(mockGetResult.getItem()).thenReturn(null);
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = credentialsRepository.read("unknownuserid");
        assertThat(result).isEmpty();
    }

    @Test
    void testReadCredentialsForUserWhoHasNotLoggedInExpectEncryptedApiKeyToBeNull() {
        when(mockGetResult.getItem()).thenReturn(Map.of("serial-number", new AttributeValue("12345678"),
                "code", new AttributeValue("abc123")));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = credentialsRepository.read("userid");
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("abc123");
        assertThat(result.get().getUserId()).isEqualTo("userid");
        assertThat(result.get().getSerialNumber()).isEqualTo("12345678");
        assertThat(result.get().getEncryptedApiKey()).isEmpty();
    }

    @Test
    void testReadCredentialsForUserWhoHasSuccessfullyLoggedInExpectEncryptedApiKeyToNotBeNull() {
        when(mockGetResult.getItem()).thenReturn(Map.of("serial-number", new AttributeValue("12345678"),
                "code", new AttributeValue("abc123"),
                "encrypted-api-key", new AttributeValue().withB(encryptedApiKey)));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = credentialsRepository.read("userid");
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("abc123");
        assertThat(result.get().getUserId()).isEqualTo("userid");
        assertThat(result.get().getSerialNumber()).isEqualTo("12345678");
        assertThat(result.get().getEncryptedApiKey()).isPresent();
        assertThat(result.get().getEncryptedApiKey()).contains(encryptedApiKey);
    }

    @Test
    void testWriteCredentialsWithNoEncryptedApiKey() {
        var creds = new ZappiCredentials("userid", "12345678", "abc123");
        credentialsRepository.write(creds);
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("zappi-login-creds");
        assertThat(putItemCaptor.getValue().getItem()).hasSize(3);
        assertThat(putItemCaptor.getValue().getItem().get("amazon-user-id").getS()).isEqualTo("userid");
        assertThat(putItemCaptor.getValue().getItem().get("serial-number").getS()).isEqualTo("12345678");
        assertThat(putItemCaptor.getValue().getItem().get("code").getS()).isEqualTo("abc123");
    }

    @Test
    void testWriteCredentialsWithEncryptedApiKey() {
        var creds = new ZappiCredentials("userid", "12345678", "abc123", encryptedApiKey);
        credentialsRepository.write(creds);
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().getItem()).hasSize(4);
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("zappi-login-creds");
        assertThat(putItemCaptor.getValue().getItem().get("amazon-user-id").getS()).isEqualTo("userid");
        assertThat(putItemCaptor.getValue().getItem().get("serial-number").getS()).isEqualTo("12345678");
        assertThat(putItemCaptor.getValue().getItem().get("code").getS()).isEqualTo("abc123");
        assertThat(putItemCaptor.getValue().getItem().get("encrypted-api-key").getB()).isEqualTo(encryptedApiKey);
    }

    @Test
    void testDeleteCredentials() {
        credentialsRepository.delete("userid");
        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue()).isNotNull();
        assertThat(deleteItemCaptor.getValue().getTableName()).isEqualTo("zappi-login-creds");
        assertThat(deleteItemCaptor.getValue().getKey().get("amazon-user-id").getS()).isEqualTo("userid");
    }
}
