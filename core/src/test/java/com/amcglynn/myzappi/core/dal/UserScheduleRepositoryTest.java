package com.amcglynn.myzappi.core.dal;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserScheduleRepositoryTest {
    @Mock
    private AmazonDynamoDB mockDb;
    @Mock
    private GetItemResult mockGetResult;

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemCaptor;
    @Captor
    private ArgumentCaptor<UpdateItemRequest> updateItemCaptor;

    private UserScheduleRepository repository;

    private final String testScheduleString = "[\n" +
            "        {\n" +
            "            \"id\": \"1234567890\",\n" +
            "            \"type\": \"RECURRING\",\n" +
            "            \"startDateTime\": \"2023-09-08T14:00\",\n" +
            "            \"zoneId\": \"Europe/Dublin\",\n" +
            "            \"days\": [\n" +
            "                1,\n" +
            "                3,\n" +
            "                5\n" +
            "            ],\n" +
            "            \"action\": {\n" +
            "                \"type\": \"chargeMode\",\n" +
            "                \"value\": \"ECO+\"\n" +
            "            }\n" +
            "        }\n" +
            "    ]";

    @BeforeEach
    void setUp() {
        repository = new UserScheduleRepository(mockDb);
    }

    @Test
    void testReadForUserWhoDoesNotExistReturnsEmptyList() {
        when(mockGetResult.getItem()).thenReturn(null);
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read(UserId.from("unknownuserid"));
        assertThat(result).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEntryInDbButEmptyScheduleList() {
        when(mockGetResult.getItem()).thenReturn(Map.of("user-id", new AttributeValue("testuser"),
                "schedules", new AttributeValue("[]")));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read(UserId.from("userid"));
        assertThat(result).isEmpty();
    }

    @Test
    void testReadForUserWhoHasEntryInDb() {
        when(mockGetResult.getItem()).thenReturn(Map.of("user-id", new AttributeValue("testuser"),
                "schedules", new AttributeValue(testScheduleString)));
        when(mockDb.getItem(any())).thenReturn(mockGetResult);
        var result = repository.read(UserId.from("userid"));
        assertThat(result).hasSize(1);
        verifySchedulesAreEqual(result.get(0), Schedule.builder().id("1234567890").type("RECURRING")
                .startDateTime(LocalDateTime.of(2023, 9, 8, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .days(List.of(1, 3, 5))
                .action(ScheduleAction.builder()
                        .type("chargeMode")
                        .value("ECO+").build())
                .build());
    }

    private void verifySchedulesAreEqual(Schedule scheduleFromDb, Schedule expected) {
        assertThat(scheduleFromDb.getId()).isEqualTo(expected.getId());
        assertThat(scheduleFromDb.getType()).isEqualTo(expected.getType());
        assertThat(scheduleFromDb.getStartDateTime()).isEqualTo(expected.getStartDateTime());
        assertThat(scheduleFromDb.getZoneId()).isEqualTo(expected.getZoneId());
        assertThat(scheduleFromDb.getDays()).isEqualTo(expected.getDays());
        var action = scheduleFromDb.getAction();
        assertThat(action.getType()).isEqualTo(expected.getAction().getType());
        assertThat(action.getValue()).isEqualTo(expected.getAction().getValue());
    }

    @Test
    void testWrite() {
        var schedules = List.of(Schedule.builder().id("1234567890").type("RECURRING")
                .startDateTime(LocalDateTime.of(2023, 9, 8, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .days(List.of(1, 3, 5))
                .action(ScheduleAction.builder()
                        .type("chargeMode")
                        .value("ECO+").build())
                .build());
        repository.write(UserId.from("testUser"), schedules);
        verify(mockDb).putItem(putItemCaptor.capture());
        assertThat(putItemCaptor.getValue()).isNotNull();
        assertThat(putItemCaptor.getValue().getItem()).hasSize(2);
        assertThat(putItemCaptor.getValue().getTableName()).isEqualTo("schedule");
        assertThat(putItemCaptor.getValue().getItem().get("user-id").getS()).isEqualTo("testUser");
        assertThat(putItemCaptor.getValue().getItem().get("schedules").getS())
                .isEqualTo(testScheduleString.replaceAll("\\s", ""));
    }

    @Test
    void testUpdate() {
        var schedules = List.of(Schedule.builder().id("1234567890").type("RECURRING")
                .startDateTime(LocalDateTime.of(2023, 9, 8, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .days(List.of(1, 3, 5))
                .action(ScheduleAction.builder()
                        .type("chargeMode")
                        .value("ECO+").build())
                .build());
        repository.update(UserId.from("testUser"), schedules);
        verify(mockDb).updateItem(updateItemCaptor.capture());
        assertThat(updateItemCaptor.getValue()).isNotNull();
        assertThat(updateItemCaptor.getValue().getTableName()).isEqualTo("schedule");
        assertThat(updateItemCaptor.getValue().getAttributeUpdates().get("schedules").getValue().getS())
                .isEqualTo("[{\"id\":\"1234567890\",\"type\":\"RECURRING\",\"startDateTime\":\"2023-09-08T14:00\"," +
                        "\"zoneId\":\"Europe/Dublin\",\"days\":[1,3,5],\"action\":{\"type\":\"chargeMode\",\"value\":\"ECO+\"}}]");
        assertThat(updateItemCaptor.getValue().getKey().get("user-id").getS()).isEqualTo("testUser");
    }
}
