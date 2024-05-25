package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.MyEnergiAccountCredentialsEncrypted;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyEnergiAccountCredentialsRepositoryTest {

    @Mock
    private AmazonDynamoDB mockDb;
    @Mock
    private GetItemResult mockGetResult;

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;

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
        when(mockGetResult.getItem()).thenReturn(null);
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = credentialsRepository.read("unknownuserid");
        assertThat(result).isEmpty();
    }

    @Test
    void testWriteCredentials() {
        var creds = new MyEnergiAccountCredentialsEncrypted("userid", encryptedEmailAddress, encryptedPassword);
        credentialsRepository.write(creds);
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().getItem()).hasSize(3);
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("myenergi-creds");
        assertThat(putItemCaptor.getValue().getItem().get("amazon-user-id").getS()).isEqualTo("userid");
        assertThat(putItemCaptor.getValue().getItem().get("encrypted-email-address").getB()).isEqualTo(encryptedEmailAddress);
        assertThat(putItemCaptor.getValue().getItem().get("encrypted-password").getB()).isEqualTo(encryptedPassword);
    }

    @Test
    void testReadCredentials() {
        when(mockGetResult.getItem()).thenReturn(Map.of(
                "encrypted-email-address", new AttributeValue().withB(encryptedEmailAddress),
                "encrypted-password", new AttributeValue().withB(encryptedPassword)));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = credentialsRepository.read("userid");
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("userid");
        assertThat(result.get().getEncryptedEmailAddress()).isEqualTo(encryptedEmailAddress);
        assertThat(result.get().getEncryptedPassword()).isEqualTo(encryptedPassword);
    }
}
