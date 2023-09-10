package com.amcglynn.myzappi.api;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionRepositoryTest {

    @Mock
    private AmazonDynamoDB mockDbClient;

    @Captor
    private ArgumentCaptor<GetItemRequest> getItemCaptor;
    @Captor
    private ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;
    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;

    @Mock
    private GetItemResult mockGetResult;

    private SessionRepository sessionRepository;
    private ByteBuffer encryptedToken;
    private final String sessionId = "03662064-99b5-404c-b4c7-a0bd04257f95";

    @BeforeEach
    void setUp() {
        encryptedToken = ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03 });
        when(mockGetResult.getItem()).thenReturn(Map.of("amazon-user-id", new AttributeValue("userId"),
                "session-id", new AttributeValue(sessionId),
                "ttl", new AttributeValue().withN("12345678"),
                "token", new AttributeValue().withB(encryptedToken)));
        when(mockDbClient.getItem(any())).thenReturn(mockGetResult);
        sessionRepository = new SessionRepository(mockDbClient);
    }

    @Test
    void readReturnsValueFromDb() {
        var result = sessionRepository.read(sessionId);

        verify(mockDbClient).getItem(getItemCaptor.capture());
        assertThat(getItemCaptor.getValue()).isNotNull();
        assertThat(getItemCaptor.getValue().getTableName()).isEqualTo("session");
        assertThat(result).isNotNull().isPresent();
        assertThat(result.get().getUserId()).isEqualTo("userId");
        assertThat(result.get().getTtl()).isEqualTo(12345678L);
        assertThat(result.get().getEncryptedToken()).isEqualTo(encryptedToken);
        assertThat(result.get().getSessionId()).isEqualTo(sessionId);
    }

    @Test
    void readReturnsEmptyOptionalWhenItDoesNotExistInTheDb() {
        when(mockGetResult.getItem()).thenReturn(null);

        assertThat(sessionRepository.read("unknownSession")).isEmpty();

        verify(mockDbClient).getItem(getItemCaptor.capture());
        assertThat(getItemCaptor.getValue()).isNotNull();
        assertThat(getItemCaptor.getValue().getTableName()).isEqualTo("session");
    }

    @Test
    void testDeleteSession() {
        sessionRepository.delete(new Session(sessionId, "userId", encryptedToken, 123L));
        verify(mockDbClient).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue()).isNotNull();
        assertThat(deleteItemCaptor.getValue().getTableName()).isEqualTo("session");
        assertThat(deleteItemCaptor.getValue().getKey().get("session-id").getS()).isEqualTo(sessionId);
    }

    @Test
    void testWriteSession() {
        sessionRepository.write(new Session(sessionId, "userId", encryptedToken, 123L));
        verify(mockDbClient).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("session");
        assertThat(putItemCaptor.getValue().getItem().get("session-id").getS()).isEqualTo(sessionId);
        assertThat(putItemCaptor.getValue().getItem().get("ttl").getN()).isEqualTo("123");
        assertThat(putItemCaptor.getValue().getItem().get("token").getB()).isEqualTo(encryptedToken);
        assertThat(putItemCaptor.getValue().getItem().get("amazon-user-id").getS()).isEqualTo("userId");
    }
}
