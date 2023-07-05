package com.amcglynn.lwa;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

/**
 * Login with Amazon client
 */
@Slf4j
public class LwaClient {

    private String getProfileUrl;
    private String getTokenInfoUrl;
    public LwaClient() {
        getProfileUrl = "https://api.amazon.com/user/profile";
        getTokenInfoUrl = "https://api.amazon.com/auth/o2/tokeninfo?access_token=";
    }

    void setGetProfileUrl(String getProfileUrl) {
        this.getProfileUrl = getProfileUrl;
    }

    void setGetTokenInfoUrl(String getTokenInfoUrl) {
        this.getTokenInfoUrl = getTokenInfoUrl;
    }

    public Optional<String> getUserId(String accessToken) {
        return getUserProfile(accessToken).map(LwaUserProfile::getUserId);
    }

    public Optional<LwaUserProfile> getUserProfile(String accessToken) {
        var request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + accessToken)
                .url(getProfileUrl)
                .build();
        return makeRequest(request, LwaUserProfile.class);
    }

    public Optional<TokenInfo> getTokenInfo(String accessToken) {
        var request = new Request.Builder()
                .url(getTokenInfoUrl + accessToken)
                .build();
        return makeRequest(request, TokenInfo.class);
    }

    public Optional<String> getTimeZone(String baseUrl, String deviceId, String accessToken) {
        var url = baseUrl + "/v2/devices/" + deviceId + "/settings/System.timeZone";
        var request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + accessToken)
                .url(url)
                .build();
        return makeRequest(request, String.class);
    }

    private <T> Optional<T> makeRequest(Request request, Class<T> clazz) {
        try {
            var client = new OkHttpClient.Builder().build();
            var response = client.newCall(request).execute();
            if (response.code() == 200) {       // a 400 will be returned if invalid
                var responseBody = new ObjectMapper()
                        .readValue(response.body().string(), clazz);
                return Optional.ofNullable(responseBody);
            }
        } catch (IOException e) {
            log.warn("Unexpected error when making request");
        }
        return Optional.empty();
    }
}
