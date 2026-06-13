package com.amcglynn.myzappi.core.dal;

import com.amcglynn.myzappi.core.model.UserId;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static com.amcglynn.myzappi.core.dal.DynamoDbAttributeValues.stringValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleDetailsRepositoryTest {
    @Mock
    private DynamoDbClient mockDb;

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;
    @Captor
    private ArgumentCaptor<DeleteItemRequest> deleteItemCaptor;

    private ScheduleDetailsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ScheduleDetailsRepository(mockDb);
    }

    @Test
    void testReadForUserWhoDoesNotExistReturnsEmptyList() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
        var result = repository.read("unknownscheduleid");
        assertThat(result).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEntryInDb() {
        when(mockDb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder()
                .item(Map.of("schedule-id", stringValue("scheduleId"),
                        "lwa-user-id", stringValue("lwaUserId")))
                .build());
        var result = repository.read("scheduleId");
        assertThat(result).isPresent();
        assertThat(result.get().getLwaUserId()).isEqualTo(UserId.from("lwaUserId"));
        assertThat(result.get().getScheduleId()).isEqualTo("scheduleId");
    }

    @Test
    void testWrite() {
        repository.write("scheduleId", UserId.from("lwaUserId"));
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().item()).hasSize(2);
        assertThat(putItemCaptor.getValue().tableName()).isEqualTo("schedule-details");
        assertThat(putItemCaptor.getValue().item().get("schedule-id").s()).isEqualTo("scheduleId");
        assertThat(putItemCaptor.getValue().item().get("lwa-user-id").s()).isEqualTo("lwaUserId");
    }

    @Test
    void testDeleteSchedule() {
        repository.delete("scheduleId");
        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue()).isNotNull();
        assertThat(deleteItemCaptor.getValue().tableName()).isEqualTo("schedule-details");
        assertThat(deleteItemCaptor.getValue().key().get("schedule-id").s()).isEqualTo("scheduleId");
    }
}
