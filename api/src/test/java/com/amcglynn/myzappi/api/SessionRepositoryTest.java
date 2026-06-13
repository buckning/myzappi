package com.amcglynn.myzappi.api;

import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.numberValue;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionRepositoryTest {

    @Mock
    private DynamoDbClient mockDbClient;

    @Captor
    private ArgumentCaptor<GetItemRequest> getItemCaptor;
    @Captor
    private ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;
    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;

    private SessionRepository sessionRepository;
    private final SessionId sessionId = SessionId.from("03662064-99b5-404c-b4c7-a0bd04257f95");

    @BeforeEach
    void setUp() {
        when(mockDbClient.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder()
                .item(Map.of("amazon-user-id", stringValue("userId"),
                        "session-id", stringValue(sessionId.toString()),
                        "ttl", numberValue("12345678")))
                .build());
        sessionRepository = new SessionRepository(mockDbClient);
    }

    @Test
    void readReturnsValueFromDb() {
        var result = sessionRepository.read(sessionId);

        verify(mockDbClient).getItem(getItemCaptor.capture());
        assertThat(getItemCaptor.getValue()).isNotNull();
        assertThat(getItemCaptor.getValue().tableName()).isEqualTo("session");
        assertThat(result).isNotNull().isPresent();
        assertThat(result.get().getUserId()).hasToString("userId");
        assertThat(result.get().getTtl()).isEqualTo(12345678L);
        assertThat(result.get().getSessionId()).isEqualTo(sessionId);
    }

    @Test
    void readReturnsEmptyOptionalWhenItDoesNotExistInTheDb() {
        when(mockDbClient.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        assertThat(sessionRepository.read(SessionId.from("unknownSession"))).isEmpty();

        verify(mockDbClient).getItem(getItemCaptor.capture());
        assertThat(getItemCaptor.getValue()).isNotNull();
        assertThat(getItemCaptor.getValue().tableName()).isEqualTo("session");
    }

    @Test
    void testDeleteSession() {
        sessionRepository.delete(new Session(sessionId, UserId.from("userId"), 123L));
        verify(mockDbClient).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue()).isNotNull();
        assertThat(deleteItemCaptor.getValue().tableName()).isEqualTo("session");
        assertThat(deleteItemCaptor.getValue().key().get("session-id").s()).isEqualTo(sessionId.toString());
    }

    @Test
    void testWriteSession() {
        sessionRepository.write(new Session(sessionId, UserId.from("userId"), 123L));
        verify(mockDbClient).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().tableName()).isEqualTo("session");
        assertThat(putItemCaptor.getValue().item().get("session-id").s()).isEqualTo(sessionId.toString());
        assertThat(putItemCaptor.getValue().item().get("ttl").n()).isEqualTo("123");
        assertThat(putItemCaptor.getValue().item().get("amazon-user-id").s()).isEqualTo("userId");
    }
}
