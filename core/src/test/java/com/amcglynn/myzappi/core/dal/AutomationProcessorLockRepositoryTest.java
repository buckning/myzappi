package com.amcglynn.myzappi.core.dal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutomationProcessorLockRepositoryTest {

    @Mock
    private DynamoDbClient mockDb;
    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;
    @Captor
    private ArgumentCaptor<UpdateItemRequest> updateItemCaptor;
    @Captor
    private ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;

    private AutomationProcessorLockRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AutomationProcessorLockRepository(mockDb);
    }

    @Test
    void acquireCreatesLockWhenNoUnexpiredLockExists() {
        var result = repository.acquire("run-1", Instant.ofEpochSecond(1000));

        assertThat(result).isTrue();
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue().tableName()).isEqualTo("automation-processor-lock");
        assertThat(putItemCaptor.getValue().item().get("lock-id").s()).isEqualTo("automation-processor");
        assertThat(putItemCaptor.getValue().item().get("runId").s()).isEqualTo("run-1");
        assertThat(putItemCaptor.getValue().item().get("expiresAt").n()).isEqualTo("1000");
        assertThat(putItemCaptor.getValue().conditionExpression())
                .isEqualTo("attribute_not_exists(#lockId) OR #expiresAt < :now");
    }

    @Test
    void acquireReturnsFalseWhenConditionalPutFails() {
        doThrow(ConditionalCheckFailedException.builder().message("locked").build())
                .when(mockDb).putItem(any(PutItemRequest.class));

        var result = repository.acquire("run-1", Instant.ofEpochSecond(1000));

        assertThat(result).isFalse();
    }

    @Test
    void refreshExtendsOnlyCurrentRunId() {
        var result = repository.refresh("run-1", Instant.ofEpochSecond(2000));

        assertThat(result).isTrue();
        verify(mockDb).updateItem(updateItemCaptor.capture());
        assertThat(updateItemCaptor.getValue().tableName()).isEqualTo("automation-processor-lock");
        assertThat(updateItemCaptor.getValue().key().get("lock-id").s()).isEqualTo("automation-processor");
        assertThat(updateItemCaptor.getValue().conditionExpression()).isEqualTo("#runId = :runId");
    }

    @Test
    void releaseDeletesOnlyCurrentRunId() {
        repository.release("run-1");

        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue().tableName()).isEqualTo("automation-processor-lock");
        assertThat(deleteItemCaptor.getValue().key().get("lock-id").s()).isEqualTo("automation-processor");
        assertThat(deleteItemCaptor.getValue().conditionExpression()).isEqualTo("#runId = :runId");
    }
}
