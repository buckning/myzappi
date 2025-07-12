package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleDetailsRepositoryTest {
    @Mock
    private AmazonDynamoDB mockDb;
    @Mock
    private GetItemResult mockGetResult;

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
        when(mockGetResult.getItem()).thenReturn(null);
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read("unknownscheduleid");
        assertThat(result).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEntryInDb() {
        when(mockGetResult.getItem()).thenReturn(Map.of("schedule-id", new AttributeValue("scheduleId"),
                "lwa-user-id", new AttributeValue("lwaUserId")));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
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
        assertThat(putItemCaptor.getValue().getItem()).hasSize(2);
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("schedule-details");
        assertThat(putItemCaptor.getValue().getItem().get("schedule-id").getS()).isEqualTo("scheduleId");
        assertThat(putItemCaptor.getValue().getItem().get("lwa-user-id").getS()).isEqualTo("lwaUserId");
    }

    @Test
    void testDeleteSchedule() {
        repository.delete("scheduleId");
        verify(mockDb).deleteItem(deleteItemCaptor.capture());
        assertThat(deleteItemCaptor.getValue()).isNotNull();
        assertThat(deleteItemCaptor.getValue().getTableName()).isEqualTo("schedule-details");
        assertThat(deleteItemCaptor.getValue().getKey().get("schedule-id").getS()).isEqualTo("scheduleId");
    }
}
