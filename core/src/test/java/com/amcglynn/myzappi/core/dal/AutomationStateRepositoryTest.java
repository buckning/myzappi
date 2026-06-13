package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.AutomationStateEntry;
import com.amcglynn.myzappi.core.model.UserId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomationStateRepositoryTest {

    @Mock
    private DynamoDbClient mockDb;
    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;
    @Captor
    private ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
    private AutomationStateRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AutomationStateRepository(mockDb);
    }

    @Test
    void readReturnsEmptyMapWhenStateRowDoesNotExist() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        var result = repository.read(UserId.from("user-1"));

        assertThat(result).isEmpty();
    }

    @Test
    void writeStoresStatesAsJsonObjectKeyedByAutomationId() throws Exception {
        var evaluatedAt = LocalDateTime.of(2026, 5, 30, 12, 0);
        repository.write(UserId.from("user-1"), Map.of("automation-1", AutomationStateEntry.builder()
                .lastPredicateMatched(true)
                .lastEvaluatedAt(evaluatedAt)
                .build()));

        verify(mockDb).putItem(putItemCaptor.capture());
        var request = putItemCaptor.getValue();
        assertThat(request.tableName()).isEqualTo("automation-state");
        assertThat(request.item().get("user-id").s()).isEqualTo("user-1");
        var states = objectMapper.readValue(request.item().get("states").s(),
                new TypeReference<Map<String, AutomationStateEntry>>() {
                });
        assertThat(states).containsKey("automation-1");
        assertThat(states.get("automation-1").getLastPredicateMatched()).isTrue();
        assertThat(states.get("automation-1").getLastEvaluatedAt()).isEqualTo(evaluatedAt);
    }

    @Test
    void deleteRemovesWholeUserStateRow() {
        repository.delete(UserId.from("user-1"));

        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue().tableName()).isEqualTo("automation-state");
        assertThat(deleteItemCaptor.getValue().key().get("user-id").s()).isEqualTo("user-1");
    }
}
