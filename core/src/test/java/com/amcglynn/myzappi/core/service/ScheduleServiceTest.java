package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.ScheduleRepository;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository mockRepository;
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ScheduleService(mockRepository);
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
        verify(mockRepository).write("mockUserId", List.of(response));
        assertThat(response.getId()).isNotNull();
        assertThat(response.getType()).isEqualTo(schedule.getType());
    }
}
