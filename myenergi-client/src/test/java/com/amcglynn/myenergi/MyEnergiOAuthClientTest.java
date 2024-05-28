package com.amcglynn.myenergi;

import com.amcglynn.myenergi.exception.ClientException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@ExtendWith(MockitoExtension.class)
class MyEnergiOAuthClientTest {
    private MyEnergiOAuthClient client;
    private MockWebServer mockWebServer;

    @BeforeEach
    public void setUp() {
        mockWebServer = new MockWebServer();
        client = new MyEnergiOAuthClient(mockWebServer.url("").uri().toString());
    }

    @Test
    void getLibbiChargeSetup() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiOAuthResponses.getLibbiChargeSetupResponse());
        mockWebServer.enqueue(mockResponse);
        var response = client.getLibbiChargeSetup("30000001");
        assertThat(response).isNotNull();
        assertThat(response.getDeviceSerial()).isEqualTo("30000001");
        assertThat(response.isChargeFromGrid()).isFalse();
        assertThat(response.getEnergyTarget()).isEqualTo(5520);
    }

    @Test
    void getLibbiChargeSetupError() {
        var mockResponse = new MockResponse()
                .setResponseCode(400)
                .setBody(MockMyEnergiOAuthResponses.getSetLibbiChargeFromGridErrorResponse());
        mockWebServer.enqueue(mockResponse);
        var clientException = catchThrowableOfType(() -> client
                .getLibbiChargeSetup("30000001"), ClientException.class);
        assertThat(clientException.getMessage())
                .isEqualTo("Failed with status code 400 and message: Device not found or user does not have access to it!");
    }

    @Test
    void setLibbiChargeFromGrid() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiOAuthResponses.getSetLibbiChargeFromGridResponse());
        mockWebServer.enqueue(mockResponse);
        client.setChargeFromGrid("30000001", true);
    }

    @Test
    void setLibbiChargeFromGridErrorResponse() {
        var mockResponse = new MockResponse()
                .setResponseCode(400)
                .setBody(MockMyEnergiOAuthResponses.getSetLibbiChargeFromGridErrorResponse());
        mockWebServer.enqueue(mockResponse);
        var clientException = catchThrowableOfType(() -> client
                .setChargeFromGrid("30000001", true), ClientException.class);
        assertThat(clientException.getMessage())
                .isEqualTo("Failed with status code 400 and message: Device not found or user does not have access to it!");
    }
}
