package com.amcglynn.myenergi;

import com.amcglynn.myenergi.exception.ServerCommunicationException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

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
            System.out.println("Failed with " + e.getMessage());
            throw new ServerCommunicationException();
        }
    }
}
