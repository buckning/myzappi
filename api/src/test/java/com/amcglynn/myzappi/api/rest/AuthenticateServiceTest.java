package com.amcglynn.myzappi.api.rest;

import com.amcglynn.lwa.TokenInfo;
import com.amcglynn.myzappi.api.Session;
import com.amcglynn.myzappi.api.SessionId;
import com.amcglynn.myzappi.api.service.AuthenticationService;
import com.amcglynn.myzappi.api.service.SessionService;
import com.amcglynn.myzappi.api.service.TokenService;
import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticateServiceTest {

    private AuthenticationService service;
    @Mock
    private TokenService mockTokenService;
    @Mock
    private SessionService mockSessionService;
    @Mock
    private TokenInfo mockTokenInfo;
    private Request request;
    private final String authToken = "Bearer Atza|tokencontentshere";
    private final SessionId sessionId = SessionId.from("03662064-99b5-404c-b4c7-a0bd04257f95");
    private final Session session = new Session(sessionId, UserId.from("testUser"), 1234L);

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(mockTokenService, mockSessionService);
        String cookieContents = "amazon_Login_state_cache=%7B%22access_token%22%3A%22Atza%tokencontentshere%22%2C%22max_age%22%" +
                "3A3300%2C%22expiration_date%22%3A1687683872618%2C%22client_id%22%3A%22amzn1.application-oa2-client.clientidhere" +
                "%22%2C%22scope%22%3A%22profile%22%7D; sessionID=03662064-99b5-404c-b4c7-a0bd04257f95";
        var headers = Map.of("cookie", cookieContents,
                "Authorization", authToken);
        request = new Request(RequestMethod.POST, "/authenticate", "{\n" +
                "    \"accessToken\": \"testToken\"\n" +
                "}", headers, Map.of());
        when(mockTokenService.getTokenInfo("Atza|tokencontentshere")).thenReturn(Optional.of(mockTokenInfo));
        when(mockSessionService.getValidSession(sessionId)).thenReturn(Optional.of(session));
        when(mockTokenInfo.getUserId()).thenReturn("testUser");
        when(mockTokenInfo.getExpires()).thenReturn(1234L);
        when(mockSessionService.createSession(UserId.from("testUser"))).thenReturn(session);
    }

    @Test
    void authenticateReturnsEmptyIfSessionIdIsNotPresentInHeaderAndNoAuthHeader() {
        request = new Request(RequestMethod.POST, "/authenticate", "{}", Map.of(), Map.of());
        when(mockSessionService.getValidSession(any())).thenReturn(Optional.empty());
        assertThat(service.authenticate(request)).isEmpty();
    }

    @Test
    void authenticateReturnsEmptyIfSessionIdIsNotPresentInHeaderAndAuthHeaderIsInvalid() {
        when(mockTokenService.getTokenInfo("tokencontentshere")).thenReturn(Optional.empty());
        var headers = Map.of("cookie", "amazon_Login_state_cache=%7B%22access_token%22%3A%22Atza%token%22%2C%22max_age%22%" +
                        "3A3300%2C%22expiration_date%22%3A1687683872618%2C%22client_id%22%3A%22amzn1.application-oa2-client.clientidhere" +
                        "%22%2C%22scope%22%3A%22profile%22%7D",
                "Authorization", "Bearer invalidToken");
        request = new Request(RequestMethod.POST, "/authenticate", "{}", headers, Map.of());
        when(mockSessionService.getValidSession(any())).thenReturn(Optional.empty());
        assertThat(service.authenticate(request)).isEmpty();
        verify(mockTokenService).getTokenInfo("invalidToken");
    }

    @Test
    void authenticateReturnsEmptyWhenAuthHeaderHasExtraTokens() {
        when(mockTokenService.getTokenInfo("tokencontentshere")).thenReturn(Optional.empty());
        var headers = Map.of("cookie", "amazon_Login_state_cache=%7B%22access_token%22%3A%22Atza%token%22%2C%22max_age%22%" +
                        "3A3300%2C%22expiration_date%22%3A1687683872618%2C%22client_id%22%3A%22amzn1.application-oa2-client.clientidhere" +
                        "%22%2C%22scope%22%3A%22profile%22%7D",
                "Authorization", "Bearer tokencontentshere Bearer extraToken");
        request = new Request(RequestMethod.POST, "/authenticate", "{}", headers, Map.of());
        when(mockSessionService.getValidSession(any())).thenReturn(Optional.empty());
        assertThat(service.authenticate(request)).isEmpty();
        verify(mockTokenService, never()).getTokenInfo(any());
    }

    @Test
    void authenticateReturnsExistingSessionWhenSessionIsValid() {
        var session = service.authenticate(request);
        assertThat(session).isPresent();
        assertThat(session.get().getSessionId()).isEqualTo(this.session.getSessionId());
        assertThat(session.get().getTtl()).isEqualTo(this.session.getTtl());
        assertThat(session.get().getUserId()).isEqualTo(this.session.getUserId());
        verify(mockTokenService, never()).getTokenInfo(any());
    }

    @Test
    void authenticateCreatesSessionWhenSessionIdIsNotPresentButBearerTokenIsValid() {
        var headersWithNoSessionId = Map.of("cookie", "amazon_Login_state_cache=%7B%22access_token%22%3A%22Atza%token%22%2C%22max_age%22%" +
                        "3A3300%2C%22expiration_date%22%3A1687683872618%2C%22client_id%22%3A%22amzn1.application-oa2-client.clientidhere" +
                        "%22%2C%22scope%22%3A%22profile%22%7D",
                "Authorization", authToken);
        request = new Request(RequestMethod.POST, "/authenticate", "{}", headersWithNoSessionId, Map.of());
        when(mockSessionService.getValidSession(any())).thenReturn(Optional.empty());
        var session = service.authenticate(request);
        assertThat(session).contains(this.session);
        verify(mockTokenService).getTokenInfo("Atza|tokencontentshere");
    }

    @Test
    void invalidateSessionCallsSessionService() {
        when(mockSessionService.getValidSession(sessionId)).thenReturn(Optional.of(session));
        service.invalidateSession(sessionId);
        verify(mockSessionService).invalidateSession(session);
    }
}
