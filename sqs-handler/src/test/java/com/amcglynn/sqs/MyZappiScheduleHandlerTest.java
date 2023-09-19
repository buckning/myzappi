package com.amcglynn.sqs;

import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.ScheduleRecurrence;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyZappiScheduleHandlerTest {

    private MyZappiScheduleHandler handler;
    @Mock
    private ScheduleService mockScheduleService;
    @Mock
    private ZappiService.Builder mockZappiServiceBuilder;
    @Mock
    private ZappiService mockZappiService;

    @BeforeEach
    void setUp() {
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockZappiService);
        handler = new MyZappiScheduleHandler(mockScheduleService, mockZappiServiceBuilder);
    }

    @Test
    void testSetChargeMode() {
        var input = new LinkedHashMap<String, String>();
        input.put("type", "setChargeMode");
        var scheduleId = UUID.randomUUID().toString();
        input.put("scheduleId", scheduleId);
        input.put("lwaUserId", "mockLwaUserId");
        when(mockScheduleService.getSchedule(scheduleId)).thenReturn(Optional.of(Schedule.builder()
                .id(scheduleId)
                .startDateTime(LocalDateTime.of(2023, 9, 13, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(new ScheduleAction("setChargeMode", "ECO"))
                .build()));
        handler.handle(new MyZappiScheduleEvent(input));
        verify(mockZappiService).setChargeMode(ZappiChargeMode.ECO);
        verify(mockScheduleService).deleteLocalSchedule(scheduleId);
    }

    @Test
    void testRecurringSchedulesAreNotDeleted() {
        var input = new LinkedHashMap<String, String>();
        input.put("type", "setChargeMode");
        var scheduleId = UUID.randomUUID().toString();
        input.put("scheduleId", scheduleId);
        input.put("lwaUserId", "mockLwaUserId");
        when(mockScheduleService.getSchedule(scheduleId)).thenReturn(Optional.of(Schedule.builder()
                .id(scheduleId)
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(new ScheduleAction("setChargeMode", "ECO"))
                        .recurrence(ScheduleRecurrence.builder()
                                .daysOfWeek(Set.of(4))
                                .timeOfDay(LocalTime.of(14, 0))
                                .build())
                .build()));
        handler.handle(new MyZappiScheduleEvent(input));
        verify(mockZappiService).setChargeMode(ZappiChargeMode.ECO);
        verify(mockScheduleService, never()).deleteLocalSchedule(anyString());
    }

    @Test
    void testSetBoostKwh() {
        var input = new LinkedHashMap<String, String>();
        input.put("type", "setBoostKwh");
        var scheduleId = UUID.randomUUID().toString();
        input.put("scheduleId", scheduleId);
        input.put("lwaUserId", "mockLwaUserId");
        when(mockScheduleService.getSchedule(scheduleId)).thenReturn(Optional.of(Schedule.builder()
                .id(scheduleId)
                .startDateTime(LocalDateTime.of(2023, 9, 13, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(new ScheduleAction("setBoostKwh", "5"))
                .build()));
        handler.handle(new MyZappiScheduleEvent(input));
        verify(mockZappiService).startBoost(new KiloWattHour(5));
    }

    @Test
    void testSetBoostUntil() {
        var input = new LinkedHashMap<String, String>();
        input.put("type", "setBoostUntil");
        var scheduleId = UUID.randomUUID().toString();
        input.put("scheduleId", scheduleId);
        input.put("lwaUserId", "mockLwaUserId");
        when(mockScheduleService.getSchedule(scheduleId)).thenReturn(Optional.of(Schedule.builder()
                .id(scheduleId)
                .startDateTime(LocalDateTime.of(2023, 9, 13, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(new ScheduleAction("setBoostUntil", "18:00")).build()));
        handler.handle(new MyZappiScheduleEvent(input));
        verify(mockZappiService).startSmartBoost(LocalTime.of(18, 0));
    }

    @Test
    void testSetBoostFor() {
        var input = new LinkedHashMap<String, String>();
        input.put("type", "setBoostFor");
        var scheduleId = UUID.randomUUID().toString();
        input.put("scheduleId", scheduleId);
        input.put("lwaUserId", "mockLwaUserId");

        // "P" indicates a period (duration).
        // "T" separates the date part (which is empty) from the time part.
        // "1H" represents 1 hour.

        when(mockScheduleService.getSchedule(scheduleId)).thenReturn(Optional.of(Schedule.builder()
                .id(scheduleId)
                .startDateTime(LocalDateTime.of(2023, 9, 13, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(new ScheduleAction("setBoostFor", "PT1H"))
                .build()));
        handler.handle(new MyZappiScheduleEvent(input));
        verify(mockZappiService).startSmartBoost(Duration.ofHours(1));
    }

    @Test
    void testUnknownType() {
        var input = new LinkedHashMap<String, String>();
        input.put("type", "unknownType");
        var scheduleId = UUID.randomUUID().toString();
        input.put("scheduleId", scheduleId);
        input.put("lwaUserId", "mockLwaUserId");
        when(mockScheduleService.getSchedule(scheduleId)).thenReturn(Optional.of(Schedule.builder()
                .id(scheduleId)
                .startDateTime(LocalDateTime.of(2023, 9, 13, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(new ScheduleAction("unknownType", "unknown"))
                .build()));
        handler.handle(new MyZappiScheduleEvent(input));
        verify(mockZappiService, never()).setChargeMode(any());
    }
}
