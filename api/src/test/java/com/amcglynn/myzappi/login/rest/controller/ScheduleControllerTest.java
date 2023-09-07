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
        var response = controller.handle(new Request(new UserId("mockUserId"), RequestMethod.POST, "/schedule", null));
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
