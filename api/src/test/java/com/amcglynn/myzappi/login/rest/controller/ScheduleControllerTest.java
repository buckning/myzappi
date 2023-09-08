package com.amcglynn.myzappi.login.rest.controller;

import com.amcglynn.myzappi.login.UserId;
import com.amcglynn.myzappi.login.rest.Request;
import com.amcglynn.myzappi.login.rest.RequestMethod;
import com.amcglynn.myzappi.login.rest.ServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ScheduleControllerTest {

    private ScheduleController controller;

    @BeforeEach
    void setUp() {
        controller = new ScheduleController();
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
    }

    @Test
    void get() {
        var response = controller.handle(new Request(new UserId("mockUserId"), RequestMethod.GET, "/schedule", null));
        assertThat(response.getStatus()).isEqualTo(200);
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
