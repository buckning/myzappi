package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutomationProcessorLockRepositoryTest {

    @Mock
    private AmazonDynamoDB mockDb;
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
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("automation-processor-lock");
        assertThat(putItemCaptor.getValue().getItem().get("lock-id").getS()).isEqualTo("automation-processor");
        assertThat(putItemCaptor.getValue().getItem().get("runId").getS()).isEqualTo("run-1");
        assertThat(putItemCaptor.getValue().getItem().get("expiresAt").getN()).isEqualTo("1000");
        assertThat(putItemCaptor.getValue().getConditionExpression())
                .isEqualTo("attribute_not_exists(#lockId) OR #expiresAt < :now");
    }

    @Test
    void acquireReturnsFalseWhenConditionalPutFails() {
        doThrow(new ConditionalCheckFailedException("locked")).when(mockDb).putItem(any(PutItemRequest.class));

        var result = repository.acquire("run-1", Instant.ofEpochSecond(1000));

        assertThat(result).isFalse();
    }

    @Test
    void refreshExtendsOnlyCurrentRunId() {
        var result = repository.refresh("run-1", Instant.ofEpochSecond(2000));

        assertThat(result).isTrue();
        verify(mockDb).updateItem(updateItemCaptor.capture());
        assertThat(updateItemCaptor.getValue().getTableName()).isEqualTo("automation-processor-lock");
        assertThat(updateItemCaptor.getValue().getKey().get("lock-id").getS()).isEqualTo("automation-processor");
        assertThat(updateItemCaptor.getValue().getConditionExpression()).isEqualTo("#runId = :runId");
    }

    @Test
    void releaseDeletesOnlyCurrentRunId() {
        repository.release("run-1");

        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue().getTableName()).isEqualTo("automation-processor-lock");
        assertThat(deleteItemCaptor.getValue().getKey().get("lock-id").getS()).isEqualTo("automation-processor");
        assertThat(deleteItemCaptor.getValue().getConditionExpression()).isEqualTo("#runId = :runId");
    }
}
