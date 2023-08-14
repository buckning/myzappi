package com.amcglynn.myzappi.login.rest;

import com.amcglynn.myzappi.login.Session;
import com.amcglynn.myzappi.login.UserId;
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
    private LogoutController mockLogutController;
    @Mock
    private Session mockSession;
    @Mock
    private Response mockResponse;

    @BeforeEach
    void setUp() {
        router = new EndpointRouter(mockHubController, mockTariffController, mockAuthController, mockLogutController);
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockTariffController.handle(any())).thenReturn(mockResponse);
        when(mockLogutController.handle(any())).thenReturn(mockResponse);
        when(mockAuthController.handle(any())).thenReturn(mockResponse);
        when(mockHubController.handle(any())).thenReturn(mockResponse);
    }

    @Test
    void authenticateRequestGetsRoutedToControllerWhenNoSessionIsPresent() {
        var request = new Request(RequestMethod.POST, "/authenticate", "{}");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockAuthController).handle(request);
    }

    @Test
    void createTariffRejectedIfNoSessionIsPresent() {
        var request = new Request(RequestMethod.POST, "/tariff", "{}");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(401);
        verify(mockTariffController, never()).handle(request);
    }

    @Test
    void createTariffRoutedToTariffControllerIfSessionIsPresent() {
        var request = new Request(new UserId("AuthenticatedUser"), RequestMethod.POST, "/tariff", "{}");
        request.setSession(mockSession);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockTariffController).handle(request);
    }

    @Test
    void getTariffRejectedIfNoSessionIsPresent() {
        var request = new Request(RequestMethod.GET, "/tariff", "{}");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(401);
        verify(mockTariffController, never()).handle(request);
    }

    @Test
    void getTariffRoutedToTariffControllerIfSessionIsPresent() {
        var request = new Request(new UserId("AuthenticatedUser"), RequestMethod.GET, "/tariff", "{}");
        request.setSession(mockSession);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockTariffController).handle(request);
    }

    @Test
    void getLogoutRejectedIfNoSessionIsPresent() {
        var request = new Request(RequestMethod.GET, "/logout", "{}");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(401);
        verify(mockLogutController, never()).handle(request);
    }

    @Test
    void getLogoutRoutedToLogoutControllerIfSessionIsPresent() {
        var request = new Request(new UserId("AuthenticatedUser"), RequestMethod.GET, "/logout", "{}");
        request.setSession(mockSession);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockLogutController).handle(request);
    }

    @Test
    void getHubRejectedIfNoSessionIsPresent() {
        var request = new Request(RequestMethod.GET, "/hub", "{}");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(401);
        verify(mockHubController, never()).handle(request);
    }

    @Test
    void postHubRejectedIfNoSessionIsPresent() {
        var request = new Request(RequestMethod.POST, "/hub", "{}");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(401);
        verify(mockHubController, never()).handle(request);
    }

    @Test
    void deleteHubRejectedIfNoSessionIsPresent() {
        var request = new Request(RequestMethod.DELETE, "/hub", "{}");
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(401);
        verify(mockHubController, never()).handle(request);
    }

    @Test
    void getHubRoutedToHubControllerIfSessionIsPresent() {
        var request = new Request(new UserId("AuthenticatedUser"), RequestMethod.GET, "/hub", "{}");
        request.setSession(mockSession);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockHubController).handle(request);
    }

    @Test
    void postHubRoutedToHubControllerIfSessionIsPresent() {
        var request = new Request(new UserId("AuthenticatedUser"), RequestMethod.POST, "/hub", "{}");
        request.setSession(mockSession);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockHubController).handle(request);
    }

    @Test
    void deleteHubRoutedToHubControllerIfSessionIsPresent() {
        var request = new Request(new UserId("AuthenticatedUser"), RequestMethod.DELETE, "/hub", "{}");
        request.setSession(mockSession);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockHubController).handle(request);
    }

    @Test
    void deleteHubRoutedToHubControllerIfLwaAccessTokenIsPresent() {
        var request = new Request(RequestMethod.DELETE, "/hub", "{}", Map.of("Authorization", "Bearer 1234"));
        when(mockAuthController.isAuthenticated(any(), eq("1234"))).thenReturn(true);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(mockHubController).handle(request);
    }

    @Test
    void deleteHubDoesNotGetRoutedToHubControllerIfLwaAccessTokenIsInvalid() {
        var request = new Request(RequestMethod.DELETE, "/hub", "{}", Map.of("Authorization", "Bearer 1234"));
        when(mockAuthController.isAuthenticated(any(), eq("1234"))).thenReturn(false);
        var response = router.route(request);
        assertThat(response.getStatus()).isEqualTo(401);
        verify(mockHubController, never()).handle(request);
    }
}
