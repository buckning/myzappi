package com.amcglynn.myzappi.api.rest;

import com.amcglynn.lwa.TokenInfo;
import com.amcglynn.myzappi.api.rest.controller.AuthenticateController;
import com.amcglynn.myzappi.api.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticateControllerTest {

    private AuthenticateController controller;
    @Mock
    private TokenService mockTokenService;
    @Mock
    private TokenInfo mockTokenInfo;
    private Request request;

    @BeforeEach
    void setUp() {
        controller = new AuthenticateController(mockTokenService);
        request = new Request(RequestMethod.POST, "/authenticate", "{\n" +
                "    \"accessToken\": \"testToken\"\n" +
                "}");
        when(mockTokenInfo.getUserId()).thenReturn("testUser");
        when(mockTokenInfo.getExpires()).thenReturn(1234L);
    }

    @Test
    void isAuthenticatedReturnsFalseIfTokenInfoIsEmpty() {
        when(mockTokenService.getTokenInfo("invalidToken")).thenReturn(Optional.empty());
        assertThat(controller.isAuthenticated(request, "invalidToken")).isFalse();
    }

    @Test
    void isAuthenticatedReturnsFalseIfTokenInfoIsValid() {
        when(mockTokenService.getTokenInfo("testToken")).thenReturn(Optional.of(mockTokenInfo));
        assertThat(controller.isAuthenticated(request, "testToken")).isTrue();
    }
}
