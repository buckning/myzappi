package com.amcglynn.myenergi;

import com.amcglynn.myenergi.apiresponse.ZappiDayHistory;
import com.amcglynn.myenergi.apiresponse.ZappiHourlyDayHistory;
import com.amcglynn.myenergi.apiresponse.ZappiStatusResponse;
import com.amcglynn.myenergi.units.KiloWattHour;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * This class allows the project to be tested without the need for a real Zappi account.
 */
public class MockMyEnergiClient extends MyEnergiClient {

    private static MockWebServer mockWebServer = new MockWebServer();

    public MockMyEnergiClient() {
        super("12345678","apiKey", mockWebServer.url("").uri());
    }

    @Override
    public ZappiStatusResponse getZappiStatus() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(ZappiResponse.getExampleResponse());
        mockWebServer.enqueue(mockResponse);
        return super.getZappiStatus();
    }

    @Override
    public void setZappiChargeMode(ZappiChargeMode zappiChargeMode) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.setZappiChargeMode(zappiChargeMode);
    }

    @Override
    public void boost(KiloWattHour kiloWattHour) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.boost(new KiloWattHour(kiloWattHour));
    }

    @Override
    public void boost(LocalTime endTime) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.boost(endTime);
    }

    @Override
    public void boost(LocalTime endTime, KiloWattHour kiloWattHour) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.boost(endTime, kiloWattHour);
    }

    @Override
    public void stopBoost() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.stopBoost();
    }

    @Override
    public ZappiHourlyDayHistory getZappiHourlyHistory(LocalDate localDate) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getHourlyHistoryResponse());
        mockWebServer.enqueue(mockResponse);
        return super.getZappiHourlyHistory(localDate);
    }

    @Override
    public ZappiDayHistory getZappiHistory(LocalDate localDate) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(ZappiResponse.getHistoryResponse());
        mockWebServer.enqueue(mockResponse);
        return super.getZappiHistory(localDate);
    }
}
