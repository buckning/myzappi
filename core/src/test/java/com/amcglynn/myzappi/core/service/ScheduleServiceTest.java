package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.ScheduleDetailsRepository;
import com.amcglynn.myzappi.core.dal.UserScheduleRepository;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.ScheduleDetails;
import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleResponse;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private UserScheduleRepository mockRepository;
    @Mock
    private ScheduleDetailsRepository mockScheduleDetailsRepository;
    private ScheduleService service;
    @Mock
    private SchedulerClient mockSchedulerClient;
    private final CreateScheduleResponse createScheduleResponse = CreateScheduleResponse.builder().scheduleArn("mockScheduleArn").build();

    @BeforeEach
    void setUp() {
        when(mockSchedulerClient.createSchedule(any(CreateScheduleRequest.class))).thenReturn(createScheduleResponse);
        service = new ScheduleService(mockRepository, mockScheduleDetailsRepository, mockSchedulerClient,
                "mockExecutionArn", "mockLambdaArn");
    }

    @Test
    void create() {
        var schedule = Schedule.builder()
                .type("RECURRING")
                .startDateTime(LocalDateTime.of(2023, 9, 10, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .days(List.of(1, 3, 5))
                .action(ScheduleAction.builder()
                        .type("chargeMode")
                        .value("ECO+")
                        .build())
                .build();
        var response = service.createSchedule(UserId.from("mockUserId"), schedule);
        verify(mockRepository).write(UserId.from("mockUserId"), List.of(response));
        verify(mockScheduleDetailsRepository).write(response.getId(), UserId.from("mockUserId"));
        assertThat(response.getId()).isNotNull();
        assertThat(response.getType()).isEqualTo(schedule.getType());
    }

    @Test
    void getScheduleReturnsEmptyOptionalWhenScheduleNotFoundForUser() {
        when(mockScheduleDetailsRepository.read("mockScheduleId")).thenReturn(java.util.Optional.empty());
        var result = service.getSchedule("mockScheduleId");
        assertThat(result).isEmpty();
    }

    @Test
    void getScheduleReturnsEmptyOptionalWhenScheduleIsFoundInScheduleDetailsTableButNoSchedulesFoundInUserScheduleTable() {
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.of(new ScheduleDetails("mockScheduleId", UserId.from("mockUserId"))));
        when(mockRepository.read("mockUserId")).thenReturn(List.of());
        var result = service.getSchedule("mockScheduleId");
        assertThat(result).isEmpty();
    }

    @Test
    void getScheduleReturnsEmptyOptionalWhenScheduleIsFoundInScheduleDetailsTableButNoCorrespondingScheduleFoundInUserScheduleTable() {
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.of(new ScheduleDetails("mockScheduleId", UserId.from("mockUserId"))));
        when(mockRepository.read("mockUserId")).thenReturn(List.of(Schedule.builder().id("unknown").build()));
        var result = service.getSchedule("mockScheduleId");
        assertThat(result).isEmpty();
    }

    @Test
    void getScheduleReturnsScheduleWhenThereIsAnAssociateValueInBothScheduleDetailsTableAndUserScheduleTable() {
        var schedule = Schedule.builder().id("mockScheduleId").build();
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.of(new ScheduleDetails("mockScheduleId", UserId.from("mockUserId"))));
        when(mockRepository.read("mockUserId")).thenReturn(List.of(schedule));
        var result = service.getSchedule("mockScheduleId");
        assertThat(result).contains(schedule);
    }
}
