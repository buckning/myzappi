package com.amcglynn.myenergi;

import com.amcglynn.myenergi.apiresponse.LibbiChargeSetupResponse;
import com.amcglynn.myenergi.apiresponse.MyEnergiResponse;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myenergi.exception.ServerCommunicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

@Slf4j
public class MyEnergiOAuthClient {
    private final String userPoolId = "eu-west-2_E57cCJB20";
    private final String accessToken;
    private static final String OAUTH_BASE_URL = "https://myaccount.myenergi.com";
    private final OkHttpClient client;
    private final String myEnergiBaseUrl;

    /**
     * Used in unit testing only
     */
    protected MyEnergiOAuthClient() {
        this.accessToken = null;
        this.client = null;
        this.myEnergiBaseUrl = null;
    }

    public MyEnergiOAuthClient(String email, String password) {
        var authHelper = new AuthenticationHelper(userPoolId);
        this.accessToken = authHelper.performSRPAuthentication(email, password);

        myEnergiBaseUrl = OAUTH_BASE_URL;
        client = new OkHttpClient.Builder()
                .build();
    }

    /**
     * Constructor for unit tests
     * @param baseUrl url for MockWebServer
     */
    MyEnergiOAuthClient(String baseUrl) {
        this.accessToken = "FakeToken";

        myEnergiBaseUrl = baseUrl;
        client = new OkHttpClient.Builder()
                .build();
    }

    public String getUserHubsAndDevices() {
        var response = getRequest("/api/Product/UserHubsAndDevices");
        log.info("Got response from myenergi {}", response);
        return response;
    }

    public void setChargeFromGrid(String serialNumber, boolean chargeFromGrid) {
        putRequest("/api/AccountAccess/LibbiMode?chargeFromGrid=" + chargeFromGrid + "&serialNo=" + serialNumber);
    }

    public void setTargetEnergy(String serialNumber, int targetEnergy) {
        putRequest("/api/AccountAccess/" + serialNumber + "/TargetEnergy?targetEnergy=" + targetEnergy);
    }

    public LibbiChargeSetupResponse getLibbiChargeSetup(String serialNumber) {
        var response = getRequest("/api/AccountAccess/" + serialNumber + "/LibbiChargeSetup");

        try {
            return new ObjectMapper().readValue(response,
                    new TypeReference<MyEnergiResponse<LibbiChargeSetupResponse>>(){})
                    .getContent();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

//    public String getLibbiMode(String serialNumber) {
//        var exampleResponse = """
//                {"status":true,"message":"","field":"","content":{"30000001":false}}
//                """;
//        return getRequest("/api/AccountAccess/LibbiMode?serialNo=" + serialNumber);
//    }

    // /api/AccountAccess/LibbiMode?serialNo=
    // https://myaccount.myenergi.com/api/AccountAccess/%7Blibbiseria%7D/TargetEnergy?targetEnergy=%7Bchargetarget_in_wh
    // https://myaccount.myenergi.com/api/AccountAccess/LibbiMode?chargeFromGrid=false&serialNo=11111111 - disable charge from grid
    // https://myaccount.myenergi.com/api/AccountAccess/LibbiMode?chargeFromGrid=true&serialNo=11111111 - charge from grid


    // TODO charge from grid API that takes username and password and returns the raw response body from myenergi
    // TODO stop charge from grid API that takes username and password and returns the raw response body from myenergi
    // TODO set target charge API that takes username and password and returns the parsed response body from myenergi

    // real feature needs new optional registration params for oauth creds

    private String getRequest(String endPointUrl) {
        try (var response = makeGetRequest(endPointUrl)) {
            if (!response.isSuccessful()) {
                var responseBody = new ObjectMapper()
                        .readValue(response.body().string(), new TypeReference<MyEnergiResponse<String>>(){});
                throw new ClientException(response.code(), responseBody.getMessage());
            }

            String responseBody = response.body().string();
            log.info("Got response from myenergi {}", responseBody);
            return responseBody;
        } catch (IOException e) {
            log.info("Failed with {}", e.getMessage(), e);
            throw new ServerCommunicationException();
        }
    }

    private String putRequest(String endPointUrl) {
        try (var response = makePutRequest(endPointUrl)) {
            if (!response.isSuccessful()) {
                 var responseBody = new ObjectMapper()
                        .readValue(response.body().string(), new TypeReference<MyEnergiResponse<String>>(){});
                throw new ClientException(response.code(), responseBody.getMessage());
            }
            String responseBody = response.body().string();
            log.info("Got response from myenergi {}", responseBody);
            return responseBody;
        } catch (IOException e) {
            log.info("Failed reading body from " + e.getMessage(), e);
            throw new ServerCommunicationException();
        }
    }

    private Response makePutRequest(String endPointUrl) {
        try {
            var request = new Request.Builder()
                    .url(myEnergiBaseUrl + endPointUrl)
                    .put(RequestBody.create(null, new byte[]{}))
                    .headers(new Headers.Builder()
                                .add("Authorization", "Bearer " + accessToken)
                            .build()
                    )
                    .build();
            return client.newCall(request).execute();
        } catch (IOException e) {
            log.info("Failed with " + e.getMessage(), e);
            throw new ServerCommunicationException();
        }
    }

    private Response makeGetRequest(String endPointUrl) {
        try {
            var request = new Request.Builder()
                    .url(myEnergiBaseUrl + endPointUrl)
                    .headers(new Headers.Builder()
                            .add("Authorization", "Bearer " + accessToken)
                            .build()
                    )
                    .build();
            return client.newCall(request).execute();
        } catch (IOException e) {
            log.info("Failed with " + e.getMessage(), e);
            throw new ServerCommunicationException();
        }
    }
}
