package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.ScheduleDetailsRepository;
import com.amcglynn.myzappi.core.dal.UserScheduleRepository;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleResponse;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private UserScheduleRepository mockRepository;
    @Mock
    private ScheduleDetailsRepository mockScheduleDetailsRepository;
    private ScheduleService service;
    @Mock
    private SchedulerClient mockSchedulerClient;
    private CreateScheduleResponse createScheduleResponse = CreateScheduleResponse.builder().scheduleArn("mockScheduleArn").build();

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
                .startTime(LocalTime.of(14, 0))
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
}
