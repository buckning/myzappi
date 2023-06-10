package com.amcglynn.myenergi;

import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myenergi.exception.InvalidRequestException;
import com.amcglynn.myenergi.exception.InvalidResponseFormatException;
import com.amcglynn.myenergi.exception.ServerCommunicationException;
import com.amcglynn.myenergi.units.Joule;
import com.amcglynn.myenergi.units.KiloWattHour;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(MockitoExtension.class)
class MyEnergiClientTest {

    private MyEnergiClient client;
    private MockWebServer mockWebServer;

    @BeforeEach
    public void setUp() {
        mockWebServer = new MockWebServer();
        client = new MyEnergiClient("12345678","apiKey", mockWebServer.url("").uri());
    }

    @Test
    void testGetStatus() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(ZappiResponse.getExampleResponse());
        mockWebServer.enqueue(mockResponse);
        var response = client.getZappiStatus();

        assertThat(response.getZappi()).hasSize(1);
        var zappiResponse = response.getZappi().get(0);
        assertThat(zappiResponse.getSerialNumber()).isEqualTo("12345678");
        assertThat(zappiResponse.getSolarGeneration()).isEqualTo(594);
        assertThat(zappiResponse.getChargeAddedThisSessionKwh()).isEqualTo(21.39);
        assertThat(zappiResponse.getEvConnectionStatus()).isEqualTo("A");
        assertThat(zappiResponse.getZappiChargeMode()).isEqualTo(3);
        assertThat(zappiResponse.getChargeStatus()).isEqualTo(ChargeStatus.PAUSED.ordinal());
    }

    @Test
    void testGetStatusThrowsInvalidResponseExceptionWhenJsonResponseIsMalformed() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(ZappiResponse.getExampleResponse().substring(10));
        mockWebServer.enqueue(mockResponse);
        var response = catchThrowable(() -> client.getZappiStatus());

        assertThat(response).isInstanceOf(InvalidResponseFormatException.class);
    }

    @Test
    void testGetStatusWithNoChargeAddedThisSession() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getExampleResponse()
                        .replace("            \"che\": 21.39,\n", ""));
        mockWebServer.enqueue(mockResponse);

        var response = client.getZappiStatus();

        assertThat(response.getZappi()).hasSize(1);
        var zappiResponse = response.getZappi().get(0);
        assertThat(zappiResponse.getSerialNumber()).isEqualTo("12345678");
        assertThat(zappiResponse.getSolarGeneration()).isEqualTo(594);
        assertThat(zappiResponse.getChargeAddedThisSessionKwh()).isEqualTo(0.0);
        assertThat(zappiResponse.getEvConnectionStatus()).isEqualTo("A");
        assertThat(zappiResponse.getZappiChargeMode()).isEqualTo(3);
        assertThat(zappiResponse.getChargeStatus()).isEqualTo(ChargeStatus.PAUSED.ordinal());
    }

    @Test
    void testBoostModeThrowsInvalidRequestExceptionWhenMinuteIsNotDivisibleBy15() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getErrorResponse());
        mockWebServer.enqueue(mockResponse);
        var endTime = LocalTime.now().withMinute(3);
        var kiloWattHour = new KiloWattHour(5);

        assertThatThrownBy(() -> client.boost(endTime, kiloWattHour))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void testSmartBoostUrlIsCorrectlyFormed() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        var endTime = LocalTime.now().withHour(2).withMinute(15);

        client.boost(endTime, new KiloWattHour(5));
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-zappi-mode-Z12345678-0-11-5-0215");
    }

    @Test
    void testSmartBoostWithOnlyEndTimeSpecifiedChangesTheChargeAmountTo99Kwh() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        var endTime = LocalTime.now().withHour(15).withMinute(45);
        client.boost(endTime);
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-zappi-mode-Z12345678-0-11-99-1545");
    }

    @Test
    void testBoostWithKiloWattHours() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        client.boost(new KiloWattHour(34));
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-zappi-mode-Z12345678-0-10-34-0000");
    }

    @Test
    void testBoostThrowsIllegalArgumentExceptionWhenBoostAmountIsNegative() {
        var throwable = catchThrowable(() -> client.boost(new KiloWattHour(-34)));
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testChangeModeBoostThrowsInvalidResponseFormatExceptionWhenServerRespondsWithUnexpectedResponse() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getGenericResponse().substring(5));
        mockWebServer.enqueue(mockResponse);
        var throwable = catchThrowable(() -> client.stopBoost());
        assertThat(throwable).isInstanceOf(InvalidResponseFormatException.class);
    }

    @Test
    void testStopBoostMode() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        client.stopBoost();
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-zappi-mode-Z12345678-0-2-0-0000");
    }

    @Test
    void testANon2xxCodeThrowsClientException() {
        var mockResponse = new MockResponse()
                .setResponseCode(400)
                .setBody(ZappiResponse.getErrorResponse());
        mockWebServer.enqueue(mockResponse);
        var response = catchThrowable(() -> client.stopBoost());
        assertThat(response).isInstanceOf(ClientException.class);
        assertThat(response.getMessage()).isEqualTo("Failed with status code 400");
    }

    @Test
    void testIOExceptionResultsInAServerCommunicationExceptionBeingThrown() throws Exception {
        mockWebServer.shutdown();
        var response = catchThrowable(() -> client.stopBoost());
        assertThat(response).isInstanceOf(ServerCommunicationException.class);
    }

    @MethodSource("chargeModesAndExpectedUrls")
    @ParameterizedTest
    void testZappiChargeMode(ZappiChargeMode zappiChargeMode, String expectedUrl, String responseBody) throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody);
        mockWebServer.enqueue(mockResponse);
        client.setZappiChargeMode(zappiChargeMode);
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains(expectedUrl);
    }

    @Test
    void testGetHourlyHistory() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getHourlyHistoryResponse());
        mockWebServer.enqueue(mockResponse);
        var response = client.getZappiHourlyHistory(LocalDate.of(2023, Month.JANUARY, 20));
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-jdayhour-Z12345678-2023-1-20");
        assertThat(response.getExpectedReadings()).isEqualTo(24);
        assertThat(response.getReadings()).hasSize(24);
    }

    @Test
    void testGetHourlyHistoryThrowsInvalidResponseFormatExceptionWhenServerRespondsWithMalformedData() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getHourlyHistoryResponse().substring(8));
        mockWebServer.enqueue(mockResponse);
        var throwable = catchThrowable(() -> client.getZappiHourlyHistory(LocalDate.of(2023, Month.JANUARY, 20)));
        assertThat(throwable).isInstanceOf(InvalidResponseFormatException.class);
    }

    @Test
    void testGetHistory() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getHistoryResponse());
        mockWebServer.enqueue(mockResponse);
        var response = client.getZappiHistory(LocalDate.of(2023, Month.JANUARY, 20));
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-jday-Z12345678-2023-1-20");
        assertThat(response.getExpectedReadings()).isEqualTo(1440);
        assertThat(response.getReadings()).hasSize(5);  // reduced down to save space in file

        var reading = response.getReadings().get(0);
        assertThat(reading.getDayOfMonth()).isEqualTo(20);
        assertThat(reading.getYear()).isEqualTo(2023);
        assertThat(reading.getMonth()).isEqualTo(1);
        assertThat(reading.getHour()).isZero();
    }

    @Test
    void testGetHistoryThrowsInvalidResponseFormatExceptionWhenServerRespondsWithMalformedData() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getHistoryResponse().substring(8));
        mockWebServer.enqueue(mockResponse);
        var throwable = catchThrowable(() -> client.getZappiHistory(LocalDate.of(2023, Month.JANUARY, 20)));
        assertThat(throwable).isInstanceOf(InvalidResponseFormatException.class);
    }

    private static Stream<Arguments> chargeModesAndExpectedUrls() {
        return Stream.of(
                Arguments.of(ZappiChargeMode.FAST, "/cgi-zappi-mode-Z12345678-1-0-0-0000", ZappiResponse.getGenericResponse()),
                Arguments.of(ZappiChargeMode.ECO_PLUS, "/cgi-zappi-mode-Z12345678-3-0-0-0000", ZappiResponse.getGenericResponse()),
                Arguments.of(ZappiChargeMode.ECO, "/cgi-zappi-mode-Z12345678-2-0-0-0000", ZappiResponse.getGenericResponse()),
                Arguments.of(ZappiChargeMode.STOP, "/cgi-zappi-mode-Z12345678-4-0-0-0000", ZappiResponse.getGenericResponse())
        );
    }
}