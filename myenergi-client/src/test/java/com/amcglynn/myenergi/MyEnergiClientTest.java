package com.amcglynn.myenergi;

import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myenergi.exception.InvalidRequestException;
import com.amcglynn.myenergi.exception.InvalidResponseFormatException;
import com.amcglynn.myenergi.exception.ServerCommunicationException;
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

import java.time.Duration;
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
        client = new MyEnergiClient("56781234", "12345678", "09876543", "apiKey", mockWebServer.url("").uri());
    }

    @Test
    void testGetZappiStatus() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(MockMyEnergiResponses.getExampleResponse());
        mockWebServer.enqueue(mockResponse);
        var response = client.getZappiStatus();

        assertThat(response.getZappi()).hasSize(1);
        var zappiResponse = response.getZappi().get(0);
        assertThat(zappiResponse.getSerialNumber()).isEqualTo("10000001");
        assertThat(zappiResponse.getSolarGeneration()).isEqualTo(594);
        assertThat(zappiResponse.getChargeAddedThisSessionKwh()).isEqualTo(21.39);
        assertThat(zappiResponse.getEvConnectionStatus()).isEqualTo("A");
        assertThat(zappiResponse.getZappiChargeMode()).isEqualTo(3);
        assertThat(zappiResponse.getChargeStatus()).isEqualTo(ChargeStatus.PAUSED.ordinal());
        assertThat(zappiResponse.getLockStatus()).isEqualTo(LockStatus.LOCKED.getCode());
    }

    @Test
    void testGetStatus() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(MockMyEnergiResponses.getExampleStatusResponse());
        mockWebServer.enqueue(mockResponse);
        var response = client.getStatus();

        assertThat(response).hasSize(5);
        var zappis = response.stream().filter(statusResponse -> statusResponse.getZappi() != null).findFirst();
        var eddis = response.stream().filter(statusResponse -> statusResponse.getEddi() != null).findFirst();
        var harvis = response.stream().filter(statusResponse -> statusResponse.getHarvi() != null).findFirst();
        var libbis = response.stream().filter(statusResponse -> statusResponse.getLibbi() != null).findFirst();
        var hub = response.stream().filter(statusResponse -> statusResponse.getAsn() != null
                && statusResponse.getFwv() != null && statusResponse.getVhub() != null).findFirst();

        assertThat(zappis).isPresent();
        assertThat(eddis).isPresent();
        assertThat(harvis).isPresent();
        assertThat(libbis).isPresent();
        assertThat(hub).isPresent();

        assertThat(zappis.get().getZappi()).hasSize(1);
        assertThat(zappis.get().getZappi().get(0).getSerialNumber()).isEqualTo("10000001");

        assertThat(libbis.get().getLibbi()).isEmpty();

        assertThat(eddis.get().getEddi()).hasSize(1);
        assertThat(eddis.get().getEddi().get(0).getSerialNumber()).isEqualTo("20000001");
        assertThat(eddis.get().getEddi().get(0).getTank1Name()).isEqualTo("Tank 1");
        assertThat(eddis.get().getEddi().get(0).getTank2Name()).isEqualTo("Tank 2");

        assertThat(harvis.get().getHarvi()).hasSize(1);
        assertThat(harvis.get().getHarvi().get(0).getSerialNumber()).isEqualTo("40000001");

        assertThat(hub.get().getAsn()).isEqualTo("s18.myenergi.net");
        assertThat(hub.get().getVhub()).isEqualTo(1);
        assertThat(hub.get().getFwv()).isEqualTo("3401S5.044");
    }

    @Test
    void testGetStatusWithZappiEddiAndLibbi() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(MockMyEnergiResponses.getExampleJStatusResponseWithZappiEddiAndLibbiButNoHarvi());
        mockWebServer.enqueue(mockResponse);
        var response = client.getStatus();

        assertThat(response).hasSize(5);
        var zappis = response.stream().filter(statusResponse -> statusResponse.getZappi() != null).findFirst();
        var eddis = response.stream().filter(statusResponse -> statusResponse.getEddi() != null).findFirst();
        var harvis = response.stream().filter(statusResponse -> statusResponse.getHarvi() != null).findFirst();
        var libbis = response.stream().filter(statusResponse -> statusResponse.getLibbi() != null).findFirst();
        var hub = response.stream().filter(statusResponse -> statusResponse.getAsn() != null
                && statusResponse.getFwv() != null && statusResponse.getVhub() != null).findFirst();

        assertThat(zappis).isPresent();
        assertThat(eddis).isPresent();
        assertThat(harvis).isPresent();
        assertThat(libbis).isPresent();
        assertThat(hub).isPresent();

        assertThat(zappis.get().getZappi()).hasSize(1);
        assertThat(zappis.get().getZappi().get(0).getSerialNumber()).isEqualTo("10000001");

        assertThat(libbis.get().getLibbi().get(0).getSerialNumber()).isEqualTo("30000001");

        assertThat(eddis.get().getEddi()).hasSize(1);
        assertThat(eddis.get().getEddi().get(0).getSerialNumber()).isEqualTo("20000001");
        assertThat(eddis.get().getEddi().get(0).getTank1Name()).isEqualTo("Tank 1");
        assertThat(eddis.get().getEddi().get(0).getTank2Name()).isEqualTo("Tank 2");

        assertThat(harvis.get().getHarvi()).isEmpty();

        assertThat(hub.get().getAsn()).isEqualTo("s18.myenergi.net");
        assertThat(hub.get().getVhub()).isEqualTo(1);
        assertThat(hub.get().getFwv()).isEqualTo("3402S5.433");
    }

    @Test
    void test401ResponseResultsInClientExceptionBeingThrown() {
        var mockResponse = new MockResponse()
                .setResponseCode(401)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(MockMyEnergiResponses.getErrorResponse());
        mockWebServer.enqueue(mockResponse);
        var throwable = catchThrowable(() -> client.getZappiStatus());

        assertThat(throwable).isInstanceOf(ClientException.class);
    }

    @Test
    void testGetStatusThrowsInvalidResponseExceptionWhenJsonResponseIsMalformed() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(MockMyEnergiResponses.getExampleResponse().substring(10));
        mockWebServer.enqueue(mockResponse);
        var response = catchThrowable(() -> client.getZappiStatus());

        assertThat(response).isInstanceOf(InvalidResponseFormatException.class);
    }

    @Test
    void testGetStatusWithNoChargeAddedThisSession() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getExampleResponse()
                        .replace("            \"che\": 21.39,\n", ""));
        mockWebServer.enqueue(mockResponse);

        var response = client.getZappiStatus();

        assertThat(response.getZappi()).hasSize(1);
        var zappiResponse = response.getZappi().get(0);
        assertThat(zappiResponse.getSerialNumber()).isEqualTo("10000001");
        assertThat(zappiResponse.getSolarGeneration()).isEqualTo(594);
        assertThat(zappiResponse.getChargeAddedThisSessionKwh()).isEqualTo(0.0);
        assertThat(zappiResponse.getEvConnectionStatus()).isEqualTo("A");
        assertThat(zappiResponse.getZappiChargeMode()).isEqualTo(3);
        assertThat(zappiResponse.getChargeStatus()).isEqualTo(ChargeStatus.PAUSED.ordinal());
    }

    @Test
    void testGetStatusWithNoLockStatus() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getExampleResponse()
                        .replace("            \"lck\": 7,\n", ""));
        mockWebServer.enqueue(mockResponse);

        var response = client.getZappiStatus();

        assertThat(response.getZappi()).hasSize(1);
        var zappiResponse = response.getZappi().get(0);
        assertThat(zappiResponse.getSerialNumber()).isEqualTo("10000001");
        assertThat(zappiResponse.getLockStatus()).isEqualTo(LockStatus.UNKNOWN.getCode());

    }

    @Test
    void testBoostModeThrowsInvalidRequestExceptionWhenMinuteIsNotDivisibleBy15() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getErrorResponse());
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
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        var endTime = LocalTime.now().withHour(2).withMinute(15);

        client.boost(endTime, new KiloWattHour(5));
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-zappi-mode-Z56781234-0-11-5-0215");
    }

    @Test
    void testSmartBoostWithOnlyEndTimeSpecifiedChangesTheChargeAmountTo99Kwh() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        var endTime = LocalTime.now().withHour(15).withMinute(45);
        client.boost(endTime);
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-zappi-mode-Z56781234-0-11-99-1545");
    }

    @Test
    void testBoostWithKiloWattHours() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        client.boost(new KiloWattHour(34));
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-zappi-mode-Z56781234-0-10-34-0000");
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
                .setBody(MockMyEnergiResponses.getGenericResponse().substring(5));
        mockWebServer.enqueue(mockResponse);
        var throwable = catchThrowable(() -> client.stopBoost());
        assertThat(throwable).isInstanceOf(InvalidResponseFormatException.class);
    }

    @Test
    void testStopBoostMode() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        client.stopBoost();
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-zappi-mode-Z56781234-0-2-0-0000");
    }

    @Test
    void testANon2xxCodeThrowsClientException() {
        var mockResponse = new MockResponse()
                .setResponseCode(400)
                .setBody(MockMyEnergiResponses.getErrorResponse());
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
                .setBody(MockMyEnergiResponses.getHourlyHistoryResponse());
        mockWebServer.enqueue(mockResponse);
        var response = client.getZappiHourlyHistory(LocalDate.of(2023, Month.JANUARY, 20));
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-jdayhour-Z56781234-2023-1-20");
        assertThat(response.getExpectedReadings()).isEqualTo(24);
        assertThat(response.getReadings()).hasSize(24);
    }

    @Test
    void testGetHourlyHistoryThrowsInvalidResponseFormatExceptionWhenServerRespondsWithMalformedData() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getHourlyHistoryResponse().substring(8));
        mockWebServer.enqueue(mockResponse);
        var throwable = catchThrowable(() -> client.getZappiHourlyHistory(LocalDate.of(2023, Month.JANUARY, 20)));
        assertThat(throwable).isInstanceOf(InvalidResponseFormatException.class);
    }

    @Test
    void testGetHistory() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getHistoryResponse());
        mockWebServer.enqueue(mockResponse);
        var response = client.getZappiHistory(LocalDate.of(2023, Month.JANUARY, 20));
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-jday-Z56781234-2023-1-20");
        assertThat(response.getExpectedReadings()).isEqualTo(1440);
        assertThat(response.getReadings()).hasSize(5);  // reduced down to save space in file

        var reading = response.getReadings().get(0);
        assertThat(reading.getDayOfMonth()).isEqualTo(20);
        assertThat(reading.getYear()).isEqualTo(2023);
        assertThat(reading.getMonth()).isEqualTo(1);
        assertThat(reading.getHour()).isZero();
    }

    @Test
    void testGetHistoryThrowsInvalidResponseFormatExceptionWhenServerRespondsWithMalformedData() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getHistoryResponse().substring(8));
        mockWebServer.enqueue(mockResponse);
        var throwable = catchThrowable(() -> client.getZappiHistory(LocalDate.of(2023, Month.JANUARY, 20)));
        assertThat(throwable).isInstanceOf(InvalidResponseFormatException.class);
    }

    @Test
    void testBoostEddi() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        client.boostEddi(Duration.ofMinutes(35));
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-eddi-boost-E09876543-10-1-35");
    }

    @Test
    void testStopBoostEddi() throws Exception {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        client.stopEddiBoost();
        var request = mockWebServer.takeRequest();
        assertThat(request.getRequestUrl().url().getPath()).contains("/cgi-eddi-boost-E09876543-1-1-0");
    }

    private static Stream<Arguments> chargeModesAndExpectedUrls() {
        return Stream.of(
                Arguments.of(ZappiChargeMode.FAST, "/cgi-zappi-mode-Z56781234-1-0-0-0000", MockMyEnergiResponses.getGenericResponse()),
                Arguments.of(ZappiChargeMode.ECO_PLUS, "/cgi-zappi-mode-Z56781234-3-0-0-0000", MockMyEnergiResponses.getGenericResponse()),
                Arguments.of(ZappiChargeMode.ECO, "/cgi-zappi-mode-Z56781234-2-0-0-0000", MockMyEnergiResponses.getGenericResponse()),
                Arguments.of(ZappiChargeMode.STOP, "/cgi-zappi-mode-Z56781234-4-0-0-0000", MockMyEnergiResponses.getGenericResponse())
        );
    }
}
