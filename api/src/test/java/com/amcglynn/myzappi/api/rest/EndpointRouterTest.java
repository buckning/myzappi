package com.amcglynn.myzappi.api.rest;

import com.amcglynn.myzappi.api.Session;
import com.amcglynn.myzappi.api.SessionId;
import com.amcglynn.myzappi.api.rest.controller.DevicesController;
import com.amcglynn.myzappi.api.rest.controller.EnergyCostController;
import com.amcglynn.myzappi.api.rest.controller.LogoutController;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.api.service.AuthenticationService;
import com.amcglynn.myzappi.api.rest.controller.EndpointRouter;
import com.amcglynn.myzappi.api.rest.controller.HubController;
import com.amcglynn.myzappi.api.rest.controller.ScheduleController;
import com.amcglynn.myzappi.api.rest.controller.TariffController;
import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EndpointRouterTest {

    private EndpointRouter router;
    @Mock
    private HubController mockHubController;
    @Mock
    private TariffController mockTariffController;
    @Mock
    private AuthenticationService mockAuthController;
    @Mock
    private ScheduleController mockScheduleController;
    @Mock
    private EnergyCostController mockEnergyCostController;
    @Mock
    private DevicesController mockDevicesController;
    @Mock
    private LogoutController mockLogoutController;
    @Mock
    private Properties mockProperties;
    @Mock
    private Response mockResponse;

    @BeforeEach
    void setUp() {
        router = new EndpointRouter(mockHubController, mockDevicesController, mockTariffController, mockAuthController,
                mockScheduleController, mockEnergyCostController, mockLogoutController, mockProperties);
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockResponse.getHeaders()).thenReturn(new HashMap<>());
        when(mockProperties.getAdminUser()).thenReturn("regularUser");
        when(mockTariffController.handle(any())).thenReturn(mockResponse);
        when(mockScheduleController.handle(any())).thenReturn(mockResponse);
        when(mockDevicesController.handle(any())).thenReturn(mockResponse);
        when(mockHubController.handle(any())).thenReturn(mockResponse);
        when(mockAuthController.authenticate(any())).thenReturn(Optional.of(new Session(SessionId.from("1234"), UserId.from("userId"), 3600L)));
        when(mockEnergyCostController.handle(any())).thenReturn(mockResponse);
    }

    @Test
    void returns404WhenEndpointNotFound() {
        var request = new Request(RequestMethod.GET, "/not-found", "{}");
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(404);
        verify(mockTariffController, never()).handle(request);
    }

    @Test
    void sessionIdCookieNotSetWhenSessionWasInTheRequest() {
        var request = new Request(RequestMethod.DELETE, "/hub", "{}", Map.of("Authorization", "Bearer 1234",
                "cookie", "sessionID=1234"), Map.of());
        when(mockAuthController.getSessionIdFromCookie(request.getHeaders())).thenReturn(Optional.of(SessionId.from("1234")));
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockHubController).handle(request);
        assertThat(response.getHeaders().get("Set-Cookie")).isNull();
    }

    @Test
    void getScheduleGetsRoutedToScheduleController() {
        var request = new Request(RequestMethod.GET, "/schedules", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockScheduleController).handle(request);
    }

    @Test
    void getDevicesGetsRoutedToDevicesController() {
        var request = new Request(RequestMethod.GET, "/devices", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockDevicesController).handle(request);
    }

    @Test
    void getDeviceGetsRoutedToDevicesController() {
        var request = new Request(RequestMethod.GET, "/devices/12345678", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockDevicesController).handle(request);
    }

    @Test
    void getSpecificScheduleGetsRoutedToScheduleController() {
        var request = new Request(RequestMethod.GET, "/schedules/1234", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockScheduleController).handle(request);
    }

    @Test
    void getEnergyCostGetsRoutedToEnergyCostController() {
        var request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockEnergyCostController).handle(request);
    }

    @Test
    void callApiWithAdminUserExpectAdminUserBehaviourWhenNoOnBehalfOfHeaderSet() {
        when(mockProperties.getAdminUser()).thenReturn("AdminUser");
        var request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("AdminUser");
        router.route(request);
        assertThat(request.getUserId()).hasToString("AdminUser");
        verify(mockEnergyCostController).handle(request);
    }

    @Test
    void callApiOnBehalfOfUserFromAdminUserExpectAdminToRunRequestAsOtherUser() {
        when(mockProperties.getAdminUser()).thenReturn("AdminUser");
        var request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of("Authorization", "Bearer 1234",
                "on-behalf-of", "RandomUser"), Map.of());
        request.setUserId("AdminUser");
        router.route(request);
        assertThat(request.getUserId()).hasToString("RandomUser");
        verify(mockEnergyCostController).handle(request);
    }

    @Test
    void callApiOnBehalfOfUserFromAdminUserWithPostRequestExpectApiToRejectNonGetRequests() {
        when(mockProperties.getAdminUser()).thenReturn("AdminUser");
        var request = new Request(RequestMethod.POST, "/energy-cost", null, Map.of("Authorization", "Bearer 1234",
                "on-behalf-of", "RandomUser"), Map.of());
        request.setUserId("AdminUser");
        router.route(request);
        assertThat(request.getUserId()).hasToString("AdminUser");
        verify(mockEnergyCostController).handle(request);
    }

    @Test
    void callApiOnBehalfOfUserFromNonAdminUserExpectNonAdminUserToNotBeAbleToRunRequestAsAnotherUser() {
        when(mockProperties.getAdminUser()).thenReturn("AdminUser");
        var request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of("Authorization", "Bearer 1234",
                "on-behalf-of", "Bob"), Map.of());
        request.setUserId("randomUser");
        router.route(request);
        assertThat(request.getUserId()).hasToString("randomUser");
        verify(mockEnergyCostController).handle(request);
    }

    @Test
    void serverExceptionStatusCodeIsReturnedWhenServerExceptionIsThrown() {
        var request = new Request(RequestMethod.GET, "/schedules/1234", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        when(mockScheduleController.handle(any())).thenThrow(new ServerException(500));
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(500);
        verify(mockScheduleController).handle(request);
    }
}
