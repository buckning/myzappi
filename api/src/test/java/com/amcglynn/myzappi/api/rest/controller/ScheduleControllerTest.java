package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.validator.ScheduleValidator;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.ScheduleRecurrence;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.ServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private ScheduleService mockService;
    @Mock
    private ScheduleValidator mockValidator;
    @Captor
    private ArgumentCaptor<Schedule> scheduleCaptor;
    private ScheduleController controller;

    @BeforeEach
    void setUp() {
        controller = new ScheduleController(mockService);
        controller.setValidator(mockValidator);
    }

    @Test
    void post() {
        String body = "{\n" +
                "    \"startDateTime\": \"2023-09-13T14:00\",\n" +
                "    \"zoneId\": \"Europe/Dublin\",\n" +
                "    \"action\": {\n" +
                "        \"type\": \"chargeMode\",\n" +
                "        \"value\": \"ECO+\"\n" +
                "    }\n" +
                "}";
        var id = UUID.randomUUID().toString();
        when(mockService.createSchedule(eq(UserId.from("mockUserId")), any())).thenReturn(Schedule.builder()
                .id(id)
                .startDateTime(LocalDateTime.of(2023, 9, 13, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(ScheduleAction.builder()
                        .type("chargeMode")
                        .value("ECO+")
                        .build())
                .build());
        var response = controller.handle(new Request(UserId.from("mockUserId"), RequestMethod.POST, "/schedule", body));
        verify(mockValidator).validate(any(Schedule.class));
        assertThat(response.getStatus()).isEqualTo(200);
        var responseBody = response.getBody();
        assertThat(responseBody).isPresent();
        assertThat(responseBody).contains("{\"id\":\"" + id + "\"," +
                "\"startDateTime\":\"2023-09-13T14:00\",\"zoneId\":\"Europe/Dublin\"," +
                "\"action\":{\"type\":\"chargeMode\",\"value\":\"ECO+\"}}");
        verify(mockService).createSchedule(eq(UserId.from("mockUserId")), any());
    }

    @Test
    void postNonRecurringSchedule() {
        String body = "{\n" +
                "    \"startDateTime\": \"2023-09-13T14:00\",\n" +
                "    \"zoneId\": \"Europe/Dublin\",\n" +
                "    \"action\": {\n" +
                "        \"type\": \"chargeMode\",\n" +
                "        \"value\": \"ECO+\"\n" +
                "    }\n" +
                "}";
        var id = UUID.randomUUID().toString();
        when(mockService.createSchedule(eq(UserId.from("mockUserId")), any())).thenReturn(Schedule.builder()
                .id(id)
                .startDateTime(LocalDateTime.of(2023, 9, 13, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(ScheduleAction.builder()
                        .type("chargeMode")
                        .value("ECO+")
                        .build())
                .build());
        var response = controller.handle(new Request(UserId.from("mockUserId"), RequestMethod.POST, "/schedule", body));
        assertThat(response.getStatus()).isEqualTo(200);
        var responseBody = response.getBody();
        assertThat(responseBody).isPresent();
        assertThat(responseBody).contains("{\"id\":\"" + id + "\"," +
                "\"startDateTime\":\"2023-09-13T14:00\",\"zoneId\":\"Europe/Dublin\"," +
                "\"action\":{\"type\":\"chargeMode\",\"value\":\"ECO+\"}}");
        verify(mockService).createSchedule(eq(UserId.from("mockUserId")), any());
    }

    @Test
    void postRecurringSchedule() {
        String body = "{\n" +
                "    \"zoneId\": \"Europe/Dublin\",\n" +
                "    \"action\": {\n" +
                "        \"type\": \"chargeMode\",\n" +
                "        \"value\": \"ECO+\"\n" +
                "    },\n" +
                "    \"recurrence\": {\n" +
                "        \"daysOfWeek\": [1, 2, 4, 7],\n" +
                "        \"timeOfDay\": \"09:30\"\n" +
                "    }\n" +
                "}";
        var id = UUID.randomUUID().toString();
        when(mockService.createSchedule(eq(UserId.from("mockUserId")), any())).thenReturn(Schedule.builder()
                .id(id)
                .zoneId(ZoneId.of("Europe/Dublin"))
                .recurrence(ScheduleRecurrence.builder()
                        .daysOfWeek(Set.of(1, 2, 4, 7))
                        .timeOfDay(LocalTime.of(9, 30))
                        .build())
                .action(ScheduleAction.builder()
                        .type("chargeMode")
                        .value("ECO+")
                        .build())
                .build());
        var response = controller.handle(new Request(UserId.from("mockUserId"), RequestMethod.POST, "/schedule", body));
        assertThat(response.getStatus()).isEqualTo(200);
        var responseBody = response.getBody();
        assertThat(responseBody).isPresent();
        assertThat(responseBody.get()).contains("{\"id\":\"" + id + "\",\"zoneId\":\"Europe/Dublin\"");
        assertThat(responseBody.get()).contains("\"action\":{\"type\":\"chargeMode\",\"value\":\"ECO+\"}");
        assertThat(responseBody.get()).contains("\"recurrence\":{\"timeOfDay\":\"09:30\",\"daysOfWeek\":[");

        verify(mockService).createSchedule(eq(UserId.from("mockUserId")), scheduleCaptor.capture());

        assertThat(scheduleCaptor.getValue().getRecurrence()).isNotNull();
        assertThat(scheduleCaptor.getValue().getRecurrence().getDaysOfWeek()).containsExactlyInAnyOrder(1, 2, 4, 7);
        assertThat(scheduleCaptor.getValue().getRecurrence().getTimeOfDay()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void get() {
        when(mockService.listSchedules(com.amcglynn.myzappi.core.model.UserId.from("mockUserId"))).thenReturn(List.of(Schedule.builder().id("1234567890")
                .startDateTime(LocalDateTime.of(2023, 9, 13, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(ScheduleAction.builder()
                        .type("chargeMode")
                        .value("ECO+").build())
                .build()));
        var response = controller.handle(new Request(UserId.from("mockUserId"), RequestMethod.GET, "/schedule", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("{\"schedules\":[{\"id\":\"1234567890\"," +
                "\"startDateTime\":\"2023-09-13T14:00\",\"zoneId\":\"Europe/Dublin\",\"action\":{\"type\":\"chargeMode\",\"value\":\"ECO+\"}}]}"));
    }

    @Test
    void delete() {
        var scheduleId = UUID.randomUUID();
        var response = controller.handle(new Request(UserId.from("mockUserId"), RequestMethod.DELETE, "/schedules/" + scheduleId, null));
        verify(mockService).deleteSchedule(UserId.from("mockUserId"), scheduleId.toString());
        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    void putUnsupported() {
        var throwable = catchThrowable(() ->
                controller.handle(new Request(UserId.from("mockUserId"), RequestMethod.PUT, "/schedule", null)));
        assertThat(throwable).isNotNull().isInstanceOf(ServerException.class);
        ServerException exception = (ServerException) throwable;
        assertThat(exception.getStatus()).isEqualTo(404);
    }
}
