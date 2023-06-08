package com.amcglynn.myzappi.core;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.dal.LoginCodeRepository;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.ZappiCredentials;
import com.amcglynn.myzappi.core.service.LoginCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginCodeRepositoryTest {

    @Mock
    private AmazonDynamoDB mockDb;
    @Mock
    private GetItemResult mockGetResult;
    private final Instant createdTime = Instant.now();

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;
    @Captor
    private ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;

    private LoginCodeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LoginCodeRepository(mockDb);
    }

    @Test
    void testReadReturnsEmptyOptionalWhenEntryDoesNotExist() {
        when(mockGetResult.getItem()).thenReturn(null);
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read(LoginCode.from("unknownlogincode"));
        assertThat(result).isEmpty();
    }

    @Test
    void testReadReturnsUserIdWhenItIsAssociatedWithCode() {
        when(mockGetResult.getItem()).thenReturn(Map.of("amazon-user-id", new AttributeValue("userid"),
                "code", new AttributeValue("abc123"),
                "created", new AttributeValue().withN(String.valueOf(createdTime.toEpochMilli()))));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read(LoginCode.from("abc123"));
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo(LoginCode.from("abc123"));
        assertThat(result.get().getUserId()).isEqualTo("userid");
        assertThat(result.get().getCreated().minus(createdTime.toEpochMilli(), ChronoUnit.MILLIS).toEpochMilli())
                .isZero();
    }

    @Test
    void testWriteCredentialsWithNoEncryptedApiKey() {
        var creds = new ZappiCredentials("userid", SerialNumber.from("12345678"), LoginCode.from("abc123"));
        repository.write(creds);
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("zappi-login-code");
        assertThat(putItemCaptor.getValue().getItem()).hasSize(3);
        assertThat(putItemCaptor.getValue().getItem().get("amazon-user-id").getS()).isEqualTo("userid");
        assertThat(putItemCaptor.getValue().getItem().get("code").getS()).isEqualTo("abc123");
        var created = putItemCaptor.getValue().getItem().get("created").getN();
        assertThat(created).isNotNull();
    }

    @Test
    void testDelete() {
        repository.delete(LoginCode.from("abc123"));
        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue()).isNotNull();
        assertThat(deleteItemCaptor.getValue().getTableName()).isEqualTo("zappi-login-code");
        assertThat(deleteItemCaptor.getValue().getKey().get("code").getS()).isEqualTo("abc123");
    }
}
