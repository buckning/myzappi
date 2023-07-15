package com.amcglynn.myzappi.login;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.login.rest.EndpointRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompleteLoginHandlerTest {

    private CompleteLoginHandler handler;

    @Mock
    private APIGatewayProxyRequestEvent mockEvent;
    @Mock
    private Context mockContext;
    @Mock
    private LoginService mockLoginService;
    @Mock
    private SessionManagementService mockSessionManagementService;
    @Mock
    private EndpointRouter mockEndpointRouter;
    @Mock
    private Properties mockProperties;

    private ByteBuffer token = ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03 });
    private Session session = new Session("03662064-99b5-404c-b4c7-a0bd04257f95", "userId", token, 3600L);

    @BeforeEach
    void setUp() {
        handler = new CompleteLoginHandler(mockLoginService, mockSessionManagementService, mockEndpointRouter, mockProperties);
    }

    @Test
    void testGetReturnsLoginButtonWhenNoSessionExists() {
        when(mockEvent.getHttpMethod()).thenReturn("GET");
        var response = handler.handleRequest(mockEvent, mockContext);
        assertThat(response).isNotNull();
        assertThat(response.getBody()).contains("<a href id=\"LoginWithAmazon\">\n" +
                "        <img border=\"0\" alt=\"Login with Amazon\"\n" +
                "             src=\"https://images-na.ssl-images-amazon.com/images/G/01/lwa/btnLWA_gold_156x32.png\"\n" +
                "             width=\"156\" height=\"32\"/>\n" +
                "    </a>");
    }

    @Test
    void testGetReturnsLogoutButtonWhenSessionExists() {
        when(mockEvent.getHttpMethod()).thenReturn("GET");
        when(mockSessionManagementService.handle(any(), any())).thenReturn(Optional.of(session));
        var response = handler.handleRequest(mockEvent, mockContext);
        assertThat(response).isNotNull();
        assertThat(response.getBody()).contains("<div class=\"container\">\n" +
                "    <button id=\"logoutButton\">Logout</button>\n" +
                "</div>");
        assertThat(response.getBody()).doesNotContain("<a href id=\"LoginWithAmazon\">\n" +
                "        <img border=\"0\" alt=\"Login with Amazon\"\n" +
                "             src=\"https://images-na.ssl-images-amazon.com/images/G/01/lwa/btnLWA_gold_156x32.png\"\n" +
                "             width=\"156\" height=\"32\"/>\n" +
                "    </a>");
    }
}
