package com.amcglynn.myenergi;

import com.amcglynn.myenergi.apiresponse.LibbiStatusResponse;
import com.amcglynn.myenergi.apiresponse.StatusResponse;
import com.amcglynn.myenergi.apiresponse.ZappiDayHistory;
import com.amcglynn.myenergi.apiresponse.ZappiHourlyDayHistory;
import com.amcglynn.myenergi.apiresponse.ZappiStatusResponse;
import com.amcglynn.myenergi.units.KiloWattHour;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * This class allows the project to be tested without the need for a real Zappi account.
 * Hub: 12345678
 * Zappis: 10000001, 10000002, 10000003
 * Eddis: 20000001, 20000002, 20000003
 * Libbis: 30000001, 30000002, 30000003
 */
public class MockMyEnergiClient extends MyEnergiClient {

    private static MockWebServer mockWebServer = new MockWebServer();
    private static final String HUB_SERIAL_NUMBER = "12345678";
    private static final String API_KEY = "myDemoApiKey";

    public MockMyEnergiClient() {
        super(HUB_SERIAL_NUMBER,API_KEY, mockWebServer.url("").uri());
    }

    @Override
    public List<StatusResponse> getStatus() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(MockMyEnergiResponses.getExampleJStatusResponseWithZappiEddiAndLibbiButNoHarvi());
        mockWebServer.enqueue(mockResponse);
        return super.getStatus();
    }

    @Override
    public ZappiStatusResponse getZappiStatus() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(MockMyEnergiResponses.getExampleResponse());
        mockWebServer.enqueue(mockResponse);
        return super.getZappiStatus();
    }

    @Override
    public ZappiStatusResponse getZappiStatus(String zappiSerialNumber) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(MockMyEnergiResponses.getExampleResponse(zappiSerialNumber));
        mockWebServer.enqueue(mockResponse);
        return super.getZappiStatus(zappiSerialNumber);
    }

    @Override
    public LibbiStatusResponse getLibbiStatus(String libbiSerialNumber) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader("x_myenergi-asn", mockWebServer.url("").uri())
                .setBody(MockMyEnergiResponses.getExampleLibbiResponse());
        mockWebServer.enqueue(mockResponse);
        return super.getLibbiStatus(libbiSerialNumber);
    }

    @Override
    public void setZappiChargeMode(ZappiChargeMode zappiChargeMode) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.setZappiChargeMode(zappiChargeMode);
    }

    @Override
    public void setLibbiMode(String serialNumber, LibbiMode libbiMode) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.setLibbiMode(serialNumber, libbiMode);
    }

    @Override
    public void boost(KiloWattHour kiloWattHour) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.boost(new KiloWattHour(kiloWattHour));
    }

    @Override
    public void boost(LocalTime endTime) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.boost(endTime);
    }

    @Override
    public void boost(LocalTime endTime, KiloWattHour kiloWattHour) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.boost(endTime, kiloWattHour);
    }

    @Override
    public void stopBoost() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.stopBoost();
    }

    @Override
    public ZappiHourlyDayHistory getZappiHourlyHistory(LocalDate localDate) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getHourlyHistoryResponse());
        mockWebServer.enqueue(mockResponse);
        return super.getZappiHourlyHistory(localDate);
    }

    @Override
    public ZappiDayHistory getZappiHistory(LocalDate localDate) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getHistoryResponse());
        mockWebServer.enqueue(mockResponse);
        return super.getZappiHistory(localDate);
    }

    @Override
    public ZappiDayHistory getZappiHistory(LocalDate localDate, int offset) {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getHistoryResponse());
        mockWebServer.enqueue(mockResponse);
        return super.getZappiHistory(localDate, offset);
    }

    @Override
    public void unlockZappi() {
        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockMyEnergiResponses.getGenericResponse());
        mockWebServer.enqueue(mockResponse);
        super.unlockZappi();
    }
}
