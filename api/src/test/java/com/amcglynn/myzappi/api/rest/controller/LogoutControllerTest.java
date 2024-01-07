package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.SessionId;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.validator.ScheduleValidator;
import com.amcglynn.myzappi.api.service.AuthenticationService;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.ScheduleRecurrence;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.ScheduleService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutControllerTest {

    @Mock
    private AuthenticationService mockService;
    private LogoutController controller;

    @BeforeEach
    void setUp() {
        controller = new LogoutController(mockService);
    }

    @Test
    void postReturns404() {
        var exception = catchThrowableOfType(() -> controller.handle(new Request(UserId.from("mockUserId"), RequestMethod.POST, "/logout", "")),
                ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(404);
    }

    @Test
    void getInvalidatesSession() {
        var headers = new HashMap<String, String>();
        headers.put("cookie", "sessionID=03662064-99b5-404c-b4c7-a0bd04257f95");
        when(mockService.getSessionIdFromCookie(any())).thenReturn(Optional.of(SessionId.from("03662064-99b5-404c-b4c7-a0bd04257f95")));
        var request = new Request(RequestMethod.GET, "/logout", "", headers, Map.of());

        var response = controller.handle(request);
        assertThat(response.getHeaders()).contains(Map.entry("Set-Cookie",
                "sessionID=03662064-99b5-404c-b4c7-a0bd04257f95;expires=Thu, Jan 01 1970 00:00:00 UTC; Path=/; Secure; SameSite=None; HttpOnly; domain=.myzappiunofficial.com"));
        verify(mockService).invalidateSession(SessionId.from("03662064-99b5-404c-b4c7-a0bd04257f95"));
    }

    @Test
    void getDoesNotInvalidateSessionIfItIsNotInDb() {
        var headers = new HashMap<String, String>();
        headers.put("cookie", "sessionID=03662064-99b5-404c-b4c7-a0bd04257f95");
        when(mockService.getSessionIdFromCookie(any())).thenReturn(Optional.empty());
        var request = new Request(RequestMethod.GET, "/logout", "", headers, Map.of());

        var response = controller.handle(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaders()).doesNotContainKey("Set-Cookie");
        verify(mockService, never()).invalidateSession(any());
    }
}
