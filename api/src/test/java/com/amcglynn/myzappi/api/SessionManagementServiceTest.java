package com.amcglynn.myzappi.api;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.myzappi.core.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionManagementServiceTest {


    @Mock
    private SessionRepository mockSessionRespository;
    @Mock
    private EncryptionService mockEncryptionService;
    @Mock
    private APIGatewayProxyRequestEvent mockEvent;
    @Mock
    private LwaClientFactory mockLwaClientFactory;
    @Mock
    private LwaClient mockLwaClient;

    private final String cookie = "amazon_Login_state_cache=%7B%22access_token%22%3A%22Atza%token%22%2C%22max_age%22%" +
            "3A3300%2C%22expiration_date%22%3A1687683872618%2C%22client_id%22%3A%22amzn1.application-oa2-client.clientidhere" +
            "%22%2C%22scope%22%3A%22profile%22%7D; sessionID=03662064-99b5-404c-b4c7-a0bd04257f95";

    @Mock
    private APIGatewayProxyResponseEvent mockResponse;

    @Captor
    private ArgumentCaptor<Map<String, String>> responseHeadersCaptor;

    private Session session;
    private ByteBuffer encryptedToken;
    private final String sessionId = "03662064-99b5-404c-b4c7-a0bd04257f95";

    private SessionManagementService sessionManagementService;

    @BeforeEach
    void setUp() {
        encryptedToken = ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03});
        session = new Session(sessionId, "userId", encryptedToken, 3600);

        when(mockEncryptionService.encrypt("lwaAccessToken")).thenReturn(encryptedToken);
        when(mockLwaClientFactory.newLwaClient()).thenReturn(mockLwaClient);
        when(mockEvent.getHeaders()).thenReturn(Map.of("cookie", cookie));
        when(mockSessionRespository.read(sessionId)).thenReturn(Optional.of(session));
        sessionManagementService = new SessionManagementService(mockSessionRespository, mockEncryptionService, mockLwaClientFactory);
        sessionManagementService.setInstantSupplier(() -> Instant.ofEpochSecond(0L));
    }

    @Test
    void handleReturnsValueFromDbWhenSessionInCookieExists() {
        var session = sessionManagementService.handle(mockEvent, mockResponse);
        assertThat(session).isNotNull().isPresent();
        assertThat(session.get().getSessionId()).isEqualTo(sessionId);
        assertThat(session.get().getTtl()).isEqualTo(3600L);
        assertThat(session.get().getUserId()).isEqualTo("userId");
        assertThat(session.get().getEncryptedToken()).isEqualTo(encryptedToken);
    }

    @Test
    void handleReturnsEmptyOptionalWhenSessionNotInDbAndNoLwaQueryParamsAreInTheUrl() {
        when(mockSessionRespository.read(sessionId)).thenReturn(Optional.empty());
        var session = sessionManagementService.handle(mockEvent, mockResponse);
        assertThat(session).isNotNull().isEmpty();
    }

    @Test
    void handleCreatesNewSessionWhenNoSessionIdIsInTheCookieButTheLwaQueryParamsAreInTheUrl() {
        when(mockEvent.getHeaders()).thenReturn(Map.of());
        when(mockEvent.getQueryStringParameters()).thenReturn(Map.of("access_token", "lwaAccessToken",
                "expires_in", "3600"));
        when(mockSessionRespository.read(sessionId)).thenReturn(Optional.empty());
        when(mockLwaClient.getUserId("lwaAccessToken")).thenReturn(Optional.of("userId"));
        var session = sessionManagementService.handle(mockEvent, mockResponse);
        assertThat(session).isNotNull().isPresent();
        assertThat(session.get().getEncryptedToken()).isEqualTo(encryptedToken);
        assertThat(session.get().getUserId()).isEqualTo("userId");
        assertThat(session.get().getSessionId()).isNotNull();
        assertThat(session.get().getTtl()).isEqualTo(3600L);

        verify(mockSessionRespository).write(session.get());
        verify(mockResponse).setHeaders(responseHeadersCaptor.capture());
        var responseHeaders = responseHeadersCaptor.getValue();
        assertThat(responseHeaders).hasSize(1).containsKey("Set-Cookie");
        assertThat(responseHeaders.get("Set-Cookie")).contains("sessionID=");   //sessionId is random each time so it is unpredictable to unit test
        assertThat(responseHeaders.get("Set-Cookie")).contains("; Max-Age=3600; Path=/; Secure; HttpOnly");
    }

    @Test
    void handleDoesNotCreateASessionWhenUserNotFoundInLwa() {
        when(mockEvent.getHeaders()).thenReturn(Map.of());
        when(mockEvent.getQueryStringParameters()).thenReturn(Map.of("access_token", "unknownAccessToken",
                "expires_in", "3600"));
        when(mockSessionRespository.read(sessionId)).thenReturn(Optional.empty());
        when(mockLwaClient.getUserId("unknownAccessToken")).thenReturn(Optional.empty());
        var session = sessionManagementService.handle(mockEvent, mockResponse);
        assertThat(session).isNotNull().isEmpty();
    }

    @Test
    void handleDoesNotReturnSessionWhenNoSessionExistsAndLwaTokenIsNotProvided() {
        when(mockEvent.getHeaders()).thenReturn(Map.of());
        when(mockEvent.getQueryStringParameters()).thenReturn(Map.of());
        var session = sessionManagementService.handle(mockEvent, mockResponse);
        assertThat(session).isNotNull().isEmpty();
    }

    @Test
    void handleDoesNotReturnSessionWhenNoSessionExistsAndLwaTokenIsProvidedButExpiresInDoesNotExist() {
        when(mockEvent.getHeaders()).thenReturn(Map.of());
        when(mockEvent.getQueryStringParameters()).thenReturn(Map.of("access_token", "myAccessToken"));
        var session = sessionManagementService.handle(mockEvent, mockResponse);
        assertThat(session).isNotNull().isEmpty();
    }

    @Test
    void handleDoesNotReturnSessionWhenNoSessionExistsAndNoQueryParametersAreInUrl() {
        when(mockEvent.getHeaders()).thenReturn(Map.of());
        when(mockEvent.getQueryStringParameters()).thenReturn(null);
        var session = sessionManagementService.handle(mockEvent, mockResponse);
        assertThat(session).isNotNull().isEmpty();
    }

    @Test
    void invalidateSessionDeletesSessionFromRepository() {
        sessionManagementService.invalidateSession(session);
        verify(mockSessionRespository).delete(session);
    }
}
