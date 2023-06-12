package com.amcglynn.myenergi;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.amcglynn.myenergi.apiresponse.GenericResponse;
import com.amcglynn.myenergi.apiresponse.ZappiDayHistory;
import com.amcglynn.myenergi.apiresponse.ZappiHourlyDayHistory;
import com.amcglynn.myenergi.apiresponse.ZappiStatusResponse;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myenergi.exception.InvalidRequestException;
import com.amcglynn.myenergi.exception.InvalidResponseFormatException;
import com.amcglynn.myenergi.exception.ServerCommunicationException;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyEnergiClient {

    private static final URI DIRECTOR_BASE_URL = URI.create("https://director.myenergi.net");
    private static final String ASN_HEADER = "x_myenergi-asn";

    private final OkHttpClient client;
    private URI baseUrl = DIRECTOR_BASE_URL;

    private final String serialNumber;
    private final LocalTime localTimeMidnight = LocalTime.now().withMinute(0).withHour(0);
    private final KiloWattHour zeroKwh = new KiloWattHour(0.0);
    private final KiloWattHour maxKwh = new KiloWattHour(99.0);

    public MyEnergiClient(String serialNumber, String apiKey) {
        this.serialNumber = serialNumber;

        var authenticator = new DigestAuthenticator(new Credentials(serialNumber, apiKey));
        Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
        client = new OkHttpClient.Builder()
                .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                .addInterceptor(new AuthenticationCacheInterceptor(authCache))
                .build();
    }

    /**
     * This should only be used by tests
     * @param serialNumber serial number of the hub device
     * @param apiKey myenergi API key
     * @param baseUrl base URL of the myenergi API
     */
    protected MyEnergiClient(String serialNumber, String apiKey, URI baseUrl) {
        this(serialNumber, apiKey);
        this.baseUrl = baseUrl;
    }

    public ZappiStatusResponse getZappiStatus() {
        var response = getRequest("/cgi-jstatus-Z" + serialNumber);
        try {
            return new ObjectMapper().readValue(response, new TypeReference<>(){});
        } catch (JsonProcessingException e) {
            throw new InvalidResponseFormatException();
        }
    }

    /**
     * Set the charge mode of Zappi. Note that this API does not take effect immediately and can take a few seconds to
     * complete, presumably because the server communicates with the Zappi asynchronously to change the mode.
     * @param zappiChargeMode the mode being switched to
     */
    public void setZappiChargeMode(ZappiChargeMode zappiChargeMode) {
        invokeCgiZappiModeApi(zappiChargeMode, ZappiBoostMode.OFF, zeroKwh, localTimeMidnight);
    }

    public void boost(KiloWattHour kiloWattHour) {
        invokeCgiZappiModeApi(ZappiChargeMode.BOOST, ZappiBoostMode.BOOST, kiloWattHour, localTimeMidnight);
    }

    public void boost(LocalTime endTime) {
        invokeCgiZappiModeApi(ZappiChargeMode.BOOST, ZappiBoostMode.SMART_BOOST, maxKwh, endTime);
    }

    public void boost(LocalTime endTime, KiloWattHour kiloWattHour) {
        invokeCgiZappiModeApi(ZappiChargeMode.BOOST, ZappiBoostMode.SMART_BOOST, kiloWattHour, endTime);
    }

    public void stopBoost() {
        invokeCgiZappiModeApi(ZappiChargeMode.BOOST, ZappiBoostMode.STOP, zeroKwh, localTimeMidnight);
    }

    private void invokeCgiZappiModeApi(ZappiChargeMode zappiChargeMode, ZappiBoostMode zappiBoostMode,
                                       KiloWattHour kiloWattHour, LocalTime endTime) {
        var kwh = validateAndClamp(kiloWattHour);

        var formatter = DateTimeFormatter.ofPattern("HHmm");
        String formattedTime = endTime.format(formatter);

        var url = "/cgi-zappi-mode-Z" + serialNumber + "-" + zappiChargeMode.getApiValue() + "-"
                + zappiBoostMode.getBoostValue() + "-" + kwh + "-" + formattedTime;
        var responseStr = getRequest(url);
        validateResponse(responseStr);
    }

    private void validateResponse(String responseStr) {
        try {
            var response = new ObjectMapper().readValue(responseStr, new TypeReference<GenericResponse>(){});
            if (response.getStatus() != 0) {
                throw new InvalidRequestException(response.getStatus(), response.getStatusText());
            }
        } catch (JsonProcessingException e) {
            throw new InvalidResponseFormatException();
        }
    }

    private int validateAndClamp(KiloWattHour kiloWattHour) {
        var kwh = (int) Math.round(kiloWattHour.getDouble());
        if (kiloWattHour.getDouble() < 0) {
            throw new IllegalArgumentException("KiloWattHours must be greater than 0");
        }
        return kwh;
    }

    public ZappiHourlyDayHistory getZappiHourlyHistory(LocalDate localDate) {
        var response = getRequest("/cgi-jdayhour-Z" + serialNumber + "-" + localDate.getYear() +
                "-" + localDate.getMonthValue()  + "-" + localDate.getDayOfMonth());
        try {
            return new ObjectMapper().readValue(response, new TypeReference<>(){});
        } catch (JsonProcessingException e) {
            throw new InvalidResponseFormatException();
        }
    }

    public ZappiDayHistory getZappiHistory(LocalDate localDate) {
        var response = getRequest("/cgi-jday-Z" + serialNumber + "-" + localDate.getYear() +
                "-" + localDate.getMonthValue() + "-" + localDate.getDayOfMonth());
        try {
            return new ObjectMapper().readValue(response, new TypeReference<>(){});
        } catch (JsonProcessingException e) {
            throw new InvalidResponseFormatException();
        }
    }

    private String getRequest(String endPointUrl) {
        try {
            var request = new Request.Builder()
                    .url(baseUrl + endPointUrl)
                    .get()
                    .build();
            var response = client.newCall(request).execute();

            handleServerRedirect(response);
            handleErrorResponse(response);
            return response.body().string();
        } catch (IOException e) {
            // okhttp throws IOException with this message if it cannot authenticate with the API
            if ("unsupported auth scheme: []".equals(e.getMessage())) {
                throw new ClientException(401);
            }
            throw new ServerCommunicationException();
        }
    }

    private void handleErrorResponse(Response response) {
        if (response.code() >= 300) {
            throw new ClientException(response.code());
        }
    }

    /**
     * If the client is communicating to the wrong server for the requested serial number, the server will return
     * the desired server through the x_myenergi-asn header. The client has to honour this and redirect all requests
     * to this server.
     * @param response response from the initial request
     */
    private void handleServerRedirect(final Response response) {
        var asnValues = response.headers().values(ASN_HEADER);
        if (asnValues.size() == 1) {
            baseUrl = URI.create(asnValues.get(0));
        }
    }
}
