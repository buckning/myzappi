package com.amcglynn.myenergi;

import com.amcglynn.myenergi.exception.ServerCommunicationException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;

@Slf4j
public class MyEnergiOAuthClient {
    private final String userPoolId = "eu-west-2_E57cCJB20";
    private final String clientId = "2fup0dhufn5vurmprjkj599041";
    private final String accessToken;
    private static final String OAUTH_BASE_URL = "https://myaccount.myenergi.com";
    private final OkHttpClient client;

    public MyEnergiOAuthClient(String email, String password) {
        var authHelper = new AuthenticationHelper(userPoolId);
        this.accessToken = authHelper.performSRPAuthentication(email, password);

        client = new OkHttpClient.Builder()
                .build();
    }

    public String getUserInfo() {
        return getRequest("/api/PersonalDetails/UserDetails");
    }

    public String setChargeFromGrid(String serialNumber, boolean chargeFromGrid) {
        return putRequest("/api/AccountAccess/LibbiMode?chargeFromGrid=" + chargeFromGrid + "&serialNo=" + serialNumber);
    }

    public String setTargetEnergy(String serialNumber, int targetEnergy) {
        return getRequest("/api/AccountAccess/" + serialNumber + "/TargetEnergy?targetEnergy=" + targetEnergy);
    }

    // https://myaccount.myenergi.com/api/AccountAccess/%7Blibbiseria%7D/TargetEnergy?targetEnergy=%7Bchargetarget_in_wh
    // https://myaccount.myenergi.com/api/AccountAccess/LibbiMode?chargeFromGrid=false&serialNo=11111111 - disable charge from grid
    // https://myaccount.myenergi.com/api/AccountAccess/LibbiMode?chargeFromGrid=true&serialNo=11111111 - charge from grid


    // TODO charge from grid API that takes username and password and returns the raw response body from myenergi
    // TODO stop charge from grid API that takes username and password and returns the raw response body from myenergi
    // TODO set target charge API that takes username and password and returns the parsed response body from myenergi

    // real feature needs new optional registration params for oauth creds


    private String getRequest(String endPointUrl) {
        try {
            var request = new Request.Builder()
                    .url(OAUTH_BASE_URL + endPointUrl)
                    .headers(new Headers.Builder()
                                .add("Authorization", "Bearer " + accessToken)
                            .build()
                    )
                    .build();
            var response = client.newCall(request).execute();

            return response.body().string();
        } catch (IOException e) {
            log.info("Failed with " + e.getMessage(), e);
            throw new ServerCommunicationException();
        }
    }

    private String putRequest(String endPointUrl) {
        try {
            var request = new Request.Builder()
                    .url(OAUTH_BASE_URL + endPointUrl)
                    .put(RequestBody.create(null, new byte[]{}))
                    .headers(new Headers.Builder()
                                .add("Authorization", "Bearer " + accessToken)
                            .build()
                    )
                    .build();
            var response = client.newCall(request).execute();

            String responseBody = response.body().string();
            log.info("Got response from myenergi {}", responseBody);
            return responseBody;
        } catch (IOException e) {
            log.info("Failed with " + e.getMessage(), e);
            throw new ServerCommunicationException();
        }
    }
}
