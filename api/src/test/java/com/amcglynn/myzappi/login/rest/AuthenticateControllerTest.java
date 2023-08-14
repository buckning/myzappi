package com.amcglynn.myzappi.login.rest;

import com.amcglynn.lwa.TokenInfo;
import com.amcglynn.myzappi.login.Session;
import com.amcglynn.myzappi.login.SessionManagementService;
import com.amcglynn.myzappi.login.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticateControllerTest {

    private AuthenticateController controller;
    @Mock
    private TokenService mockTokenService;
    @Mock
    private SessionManagementService mockSessionManagementService;
    @Mock
    private TokenInfo mockTokenInfo;
    private Request request;

    @BeforeEach
    void setUp() {
        controller = new AuthenticateController(mockTokenService, mockSessionManagementService);
        request = new Request(RequestMethod.POST, "/authenticate", "{\n" +
                "    \"accessToken\": \"testToken\"\n" +
                "}");
        when(mockTokenInfo.getUserId()).thenReturn("testUser");
        when(mockTokenInfo.getExpires()).thenReturn(1234L);
    }

    @Test
    void handleCreatesSessionWhenTokenIsValid() {
        when(mockTokenService.getTokenInfo("testToken")).thenReturn(Optional.of(mockTokenInfo));
        var session = new Session(UUID.randomUUID().toString(), "testUser", ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03 }), 1234L);
        when(mockSessionManagementService.createSession("testUser", "testToken", 1234L))
                .thenReturn(session);
        var response = controller.handle(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaders()).containsEntry("Set-Cookie", "sessionID=" + session.getSessionId() + "; Max-Age=1234; Path=/; Secure; HttpOnly");
    }

    @Test
    void handleThrows403WhenTokenIsNotValid() {
        var throwable = catchThrowable(() -> controller.handle(request));
        assertThat(throwable).isNotNull().isInstanceOf(ServerException.class);
        var serverException = (ServerException) throwable;
        assertThat(serverException.getStatus()).isEqualTo(403);
    }

    @Test
    void handleThrows400WhenAccessTokenIsMissing() {
        var throwable = catchThrowable(() -> controller.handle(new Request(RequestMethod.POST, "/authenticate", "{}")));
        assertThat(throwable).isNotNull().isInstanceOf(ServerException.class);
        var serverException = (ServerException) throwable;
        assertThat(serverException.getStatus()).isEqualTo(400);
    }
}
