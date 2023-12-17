package com.amcglynn.myzappi.api.rest;

import com.amcglynn.myzappi.api.rest.controller.EnergyCostController;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.api.rest.controller.AuthenticateController;
import com.amcglynn.myzappi.api.rest.controller.EndpointRouter;
import com.amcglynn.myzappi.api.rest.controller.HubController;
import com.amcglynn.myzappi.api.rest.controller.ScheduleController;
import com.amcglynn.myzappi.api.rest.controller.TariffController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private AuthenticateController mockAuthController;
    @Mock
    private ScheduleController mockScheduleController;
    @Mock
    private EnergyCostController mockEnergyCostController;
    @Mock
    private Properties mockProperties;
    @Mock
    private Response mockResponse;

    @BeforeEach
    void setUp() {
        router = new EndpointRouter(mockHubController, mockTariffController, mockAuthController,
                mockScheduleController, mockEnergyCostController, mockProperties);
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockProperties.getAdminUser()).thenReturn("regularUser");
        when(mockTariffController.handle(any())).thenReturn(mockResponse);
        when(mockScheduleController.handle(any())).thenReturn(mockResponse);
        when(mockHubController.handle(any())).thenReturn(mockResponse);
        when(mockEnergyCostController.handle(any())).thenReturn(mockResponse);
    }
    @Test
    void createTariffRejectedIfNoSessionIsPresent() {
        var request = new Request(RequestMethod.POST, "/tariff", "{}");
        request.setUserId("regularUser");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(401);
        verify(mockTariffController, never()).handle(request);
    }

    @Test
    void deleteHubRoutedToHubControllerIfLwaAccessTokenIsPresent() {
        var request = new Request(RequestMethod.DELETE, "/hub", "{}", Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        when(mockAuthController.isAuthenticated(any(), eq("1234"))).thenReturn(true);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockHubController).handle(request);
    }

    @Test
    void deleteHubDoesNotGetRoutedToHubControllerIfLwaAccessTokenIsInvalid() {
        var request = new Request(RequestMethod.DELETE, "/hub", "{}", Map.of("Authorization", "Bearer 1234"), Map.of());
        when(mockAuthController.isAuthenticated(any(), eq("1234"))).thenReturn(false);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(401);
        verify(mockHubController, never()).handle(request);
    }

    @Test
    void getScheduleGetsRoutedToScheduleController() {
        var request = new Request(RequestMethod.GET, "/schedule", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        when(mockAuthController.isAuthenticated(any(), eq("1234"))).thenReturn(true);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockScheduleController).handle(request);
    }

    @Test
    void getEnergyCostGetsRoutedToEnergyCostController() {
        var request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("regularUser");
        when(mockAuthController.isAuthenticated(any(), eq("1234"))).thenReturn(true);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockEnergyCostController).handle(request);
    }

    @Test
    void callApiWithAdminUserExpectAdminUserBehaviourWhenNoOnBehalfOfHeaderSet() {
        when(mockProperties.getAdminUser()).thenReturn("AdminUser");
        var request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of("Authorization", "Bearer 1234"), Map.of());
        request.setUserId("AdminUser");
        when(mockAuthController.isAuthenticated(any(), eq("1234"))).thenReturn(true);
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
        when(mockAuthController.isAuthenticated(any(), eq("1234"))).thenReturn(true);
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
        when(mockAuthController.isAuthenticated(any(), eq("1234"))).thenReturn(true);
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
        when(mockAuthController.isAuthenticated(any(), eq("1234"))).thenReturn(true);
        router.route(request);
        assertThat(request.getUserId()).hasToString("randomUser");
        verify(mockEnergyCostController).handle(request);
    }
}
