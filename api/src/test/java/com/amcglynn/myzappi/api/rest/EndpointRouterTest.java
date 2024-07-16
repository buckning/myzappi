package com.amcglynn.myzappi.api.rest;

import com.amcglynn.myzappi.api.Session;
import com.amcglynn.myzappi.api.SessionId;
import com.amcglynn.myzappi.api.rest.controller.AccountController;
import com.amcglynn.myzappi.api.rest.controller.DevicesController;
import com.amcglynn.myzappi.api.rest.controller.EnergyController;
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
    private EnergyController mockEnergyCostController;
    @Mock
    private DevicesController mockDevicesController;
    @Mock
    private AccountController mockAccountController;
    @Mock
    private LogoutController mockLogoutController;
    @Mock
    private Properties mockProperties;
    @Mock
    private Response mockResponse;

    @BeforeEach
    void setUp() {
        router = new EndpointRouter(mockHubController, mockDevicesController, mockTariffController, mockAuthController,
                mockScheduleController, mockEnergyCostController, mockLogoutController, mockAccountController, mockProperties);
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockResponse.getHeaders()).thenReturn(new HashMap<>());
        when(mockProperties.getAdminUser()).thenReturn("regularUser");
        when(mockTariffController.getTariffs(any())).thenReturn(mockResponse);
        when(mockTariffController.saveTariffs(any())).thenReturn(mockResponse);
        when(mockScheduleController.getSchedules(any())).thenReturn(mockResponse);
        when(mockScheduleController.deleteSchedule(any())).thenReturn(mockResponse);
        when(mockScheduleController.createSchedule(any())).thenReturn(mockResponse);
        when(mockDevicesController.getDevice(any())).thenReturn(mockResponse);
        when(mockDevicesController.listDevices(any())).thenReturn(mockResponse);
        when(mockDevicesController.deleteDevices(any())).thenReturn(mockResponse);
        when(mockDevicesController.setMode(any())).thenReturn(mockResponse);
        when(mockDevicesController.setLibbiTargetEnergy(any())).thenReturn(mockResponse);
        when(mockHubController.delete(any())).thenReturn(mockResponse);
        when(mockHubController.refresh(any())).thenReturn(mockResponse);
        when(mockHubController.get(any())).thenReturn(mockResponse);
        when(mockHubController.register(any())).thenReturn(mockResponse);
        when(mockEnergyCostController.getEnergyCost(any())).thenReturn(mockResponse);
        when(mockAccountController.register(any())).thenReturn(mockResponse);
        when(mockAccountController.getAccountSummary(any())).thenReturn(mockResponse);
        when(mockAuthController.authenticate(any())).thenReturn(Optional.of(new Session(SessionId.from("1234"), UserId.from("userId"), 3600L)));
    }

    @Test
    void returns404WhenHandlerNotFound() {
        var request = new Request(RequestMethod.GET, "/not-found", "{}");
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void sessionIdCookieNotSetWhenSessionWasInTheRequest() {
        var request = new Request(RequestMethod.DELETE, "/hub", "{}", Map.of("Authorization", "Bearer 1234",
                "cookie", "sessionID=1234"), Map.of());
        when(mockAuthController.getSessionIdFromCookie(request.getHeaders())).thenReturn(Optional.of(SessionId.from("1234")));
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockHubController).delete(request);
        assertThat(response.getHeaders().get("Set-Cookie")).isNull();
    }

    @Test
    void getScheduleGetsRoutedToScheduleController() {
        var request = new Request(RequestMethod.GET, "/schedules", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockScheduleController).getSchedules(request);
    }

    @Test
    void getAccountSummaryGetsRoutedToAccountController() {
        var request = new Request(RequestMethod.GET, "/account/summary", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockAccountController).getAccountSummary(request);
    }

    @Test
    void createSpecificScheduleGetsRoutedToScheduleController() {
        var request = new Request(RequestMethod.POST, "/schedules", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockScheduleController).createSchedule(request);
    }

    @Test
    void deleteSpecificScheduleGetsRoutedToScheduleController() {
        var request = new Request(RequestMethod.DELETE, "/schedules/1234", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockScheduleController).deleteSchedule(request);
    }

    @Test
    void getDevicesGetsRoutedToDevicesController() {
        var request = new Request(RequestMethod.GET, "/devices", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockDevicesController).listDevices(request);
    }

    @Test
    void getDeviceGetsRoutedToDevicesController() {
        var request = new Request(RequestMethod.GET, "/devices/12345678", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockDevicesController).getDevice(request);
    }

    @Test
    void setModeGetsRoutedToDevicesController() {
        var request = new Request(RequestMethod.PUT, "/devices/12345678/mode", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockDevicesController).setMode(request);
    }

    @Test
    void setTargetEnergyGetsRoutedToDevicesController() {
        var request = new Request(RequestMethod.PUT, "/devices/12345678/target-energy", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockDevicesController).setLibbiTargetEnergy(request);
    }

    @Test
    void getEnergyCostGetsRoutedToEnergyCostController() {
        var request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockEnergyCostController).getEnergyCost(request);
    }

    @Test
    void postRegisterAccountGetsRoutedToAccountController() {
        var request = new Request(RequestMethod.POST, "/account/register", "{}", Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockAccountController).register(request);
    }

    @Test
    void callApiWithAdminUserExpectAdminUserBehaviourWhenNoOnBehalfOfHeaderSet() {
        when(mockProperties.getAdminUser()).thenReturn("AdminUser");
        var request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("AdminUser");
        router.route(request);
        assertThat(request.getUserId()).hasToString("AdminUser");
        verify(mockEnergyCostController).getEnergyCost(request);
    }

    @Test
    void callApiOnBehalfOfUserFromAdminUserExpectAdminToRunRequestAsOtherUser() {
        when(mockProperties.getAdminUser()).thenReturn("AdminUser");
        var request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of("Authorization", "Bearer 1234",
                "on-behalf-of", "RandomUser"), Map.of());
        request.setUserId("AdminUser");
        router.route(request);
        assertThat(request.getUserId()).hasToString("RandomUser");
        verify(mockEnergyCostController).getEnergyCost(request);
    }

    @Test
    void callApiOnBehalfOfUserFromAdminUserWithPostRequestExpectApiToRejectNonGetRequests() {
        when(mockProperties.getAdminUser()).thenReturn("AdminUser");
        var request = new Request(RequestMethod.POST, "/schedules", null, Map.of("Authorization", "Bearer 1234",
                "on-behalf-of", "RandomUser"), Map.of());
        request.setUserId("AdminUser");
        router.route(request);
        assertThat(request.getUserId()).hasToString("AdminUser");
        verify(mockScheduleController).createSchedule(request);
    }

    @Test
    void callApiOnBehalfOfUserFromNonAdminUserExpectNonAdminUserToNotBeAbleToRunRequestAsAnotherUser() {
        when(mockProperties.getAdminUser()).thenReturn("AdminUser");
        var request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of("Authorization", "Bearer 1234",
                "on-behalf-of", "Bob"), Map.of());
        request.setUserId("randomUser");
        router.route(request);
        assertThat(request.getUserId()).hasToString("randomUser");
        verify(mockEnergyCostController).getEnergyCost(request);
    }

    @Test
    void serverExceptionStatusCodeIsReturnedWhenServerExceptionIsThrown() {
        var request = new Request(RequestMethod.GET, "/schedules", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        when(mockScheduleController.getSchedules(any())).thenThrow(new ServerException(500));
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(500);
        verify(mockScheduleController).getSchedules(request);
    }
}
