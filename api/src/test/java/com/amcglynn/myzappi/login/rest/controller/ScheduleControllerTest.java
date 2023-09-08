package com.amcglynn.myzappi.login.rest.controller;

import com.amcglynn.myzappi.core.dal.ScheduleRepository;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.login.UserId;
import com.amcglynn.myzappi.login.rest.Request;
import com.amcglynn.myzappi.login.rest.RequestMethod;
import com.amcglynn.myzappi.login.rest.ServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private ScheduleRepository mockRepository;
    private ScheduleController controller;

    @BeforeEach
    void setUp() {
        controller = new ScheduleController(mockRepository);
    }

    @Test
    void post() {
        String body = "{\n" +
                "    \"schedules\": [{\n" +
                "        \"id\": \"1234567890\",\n" +
                "        \"type\": \"RECURRING\",\n" +
                "        \"startTime\": \"14:00\",\n" +
                "        \"zoneId\": \"Europe/Dublin\",\n" +
                "        \"days\": [1, 3, 5],\n" +
                "        \"action\": {\n" +
                "            \"type\": \"chargeMode\",\n" +
                "            \"value\": \"ECO+\"\n" +
                "        }\n" +
                "    }, {\n" +
                "        \"id\": \"1234567890\",\n" +
                "        \"type\": \"RECURRING\",\n" +
                "        \"startTime\": \"14:00\",\n" +
                "        \"zoneId\": \"Europe/Dublin\",\n" +
                "        \"days\": [1, 2, 3, 4, 5, 6, 7],\n" +
                "        \"action\": {\n" +
                "            \"type\": \"remindCost\"\n" +
                "        }\n" +
                "    }, {\n" +
                "        \"id\": \"1234567890\",\n" +
                "        \"type\": \"RECURRING\",\n" +
                "        \"startTime\": \"14:00\",\n" +
                "        \"zoneId\": \"Europe/Dublin\",\n" +
                "        \"days\": [1, 5, 7],\n" +
                "        \"action\": {\n" +
                "            \"type\": \"remindPlugStatus\"\n" +
                "        }\n" +
                "    }, {\n" +
                "        \"id\": \"0987654321\",\n" +
                "        \"startTime\": \"15:00\",\n" +
                "        \"zoneId\": \"Europe/Dublin\",\n" +
                "        \"action\": {\n" +
                "            \"type\": \"boostKwh\",\n" +
                "            \"value\": \"5\"\n" +
                "        }\n" +
                "    }]\n" +
                "}";
        var response = controller.handle(new Request(new UserId("mockUserId"), RequestMethod.POST, "/schedule", body));
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockRepository).write(eq("mockUserId"), any());
    }

    @Test
    void get() {
        when(mockRepository.read("mockUserId")).thenReturn(List.of(Schedule.builder().id("1234567890").type("RECURRING")
                .startTime(LocalTime.of(14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .days(List.of(1, 3, 5))
                .action(ScheduleAction.builder()
                        .type("chargeMode")
                        .value("ECO+").build())
                .build()));
        var response = controller.handle(new Request(new UserId("mockUserId"), RequestMethod.GET, "/schedule", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("{\"schedules\":[{\"id\":\"1234567890\",\"type\":\"RECURRING\"," +
                "\"startTime\":\"14:00\",\"zoneId\":\"Europe/Dublin\",\"days\":[1,3,5],\"action\":{\"type\":\"chargeMode\",\"value\":\"ECO+\"}}]}"));
    }

    @Test
    void delete() {
        var response = controller.handle(new Request(new UserId("mockUserId"), RequestMethod.DELETE, "/schedule", null));
        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    void putUnsupported() {
        var throwable = catchThrowable(() ->
                controller.handle(new Request(new UserId("mockUserId"), RequestMethod.PUT, "/schedule", null)));
        assertThat(throwable).isNotNull().isInstanceOf(ServerException.class);
        ServerException exception = (ServerException) throwable;
        assertThat(exception.getStatus()).isEqualTo(404);
    }
}
