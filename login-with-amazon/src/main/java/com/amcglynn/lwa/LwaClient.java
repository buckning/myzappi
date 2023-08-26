package com.amcglynn.lwa;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import lombok.extern.slf4j.Slf4j;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.Optional;

/**
 * Login with Amazon client
 */
@Slf4j
public class LwaClient {

    private String getProfileUrl;
    private String getTokenInfoUrl;
    private String getTokenUrl;

    public LwaClient() {
        getProfileUrl = "https://api.amazon.com/user/profile";
        getTokenInfoUrl = "https://api.amazon.com/auth/o2/tokeninfo?access_token=";
        getTokenUrl = "https://api.amazon.com/auth/o2/token";
    }

    void setGetProfileUrl(String getProfileUrl) {
        this.getProfileUrl = getProfileUrl;
    }

    void setGetTokenInfoUrl(String getTokenInfoUrl) {
        this.getTokenInfoUrl = getTokenInfoUrl;
    }

    void setGetTokenUrl(String getTokenUrl) {
        this.getTokenUrl = getTokenUrl;
    }

    /**
     * Get access token for out-of-session messaging flow. This token is used with postSkillMessage
     * @param clientId client ID from Alexa Skill Messaging section from Permission page on Alexa developer Skill
     * @param clientSecret client secret from Alexa Skill Messaging section from Permission page on Alexa developer Skill
     * @return Token if request is valid
     */
    public Token getMessagingToken(String clientId, String clientSecret) {
        var formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("scope", "alexa:skill_messaging")
                .build();

        Request request = new Request.Builder()
                .url(getTokenUrl)
                .post(formBody)
                .build();
        return makeRequest(request, Token.class)
                .orElseThrow(ClientException::new);
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

    /**
     * Post an asynchronous message for services listening to Messaging.MessageReceived events. This data is routed to
     * the skill associated with the access token, which is identified by the client ID and secret of the skill.
     * A 202 is expected when the request is successfully sent to Alexa servers (but not necessarily the handling service).
     * @param baseUrl baseUrl of the local Alexa API
     * @param userId ID of the Alexa user. Note that this is not the same as the LWA linked userID
     * @param accessToken LWA token for the service. Note that this is not a user token
     * @param data bespoke message data that the consuming Alexa service will process
     */
    @SneakyThrows
    public void postSkillMessage(String baseUrl, String userId, String accessToken, Object data) {
        var url = baseUrl + "/v1/skillmessages/users/" + userId;

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        String requestBody1 = new ObjectMapper().writeValueAsString(new SkillMessageRequest(36000, data));
        RequestBody requestBody = RequestBody.create(requestBody1, mediaType);
        var request = new Request.Builder()
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + accessToken)
                .url(url)
                .build();
        makeRequest(request, 202);
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

    private void makeRequest(Request request, int expectedStatusCode) {
        try {
            var client = new OkHttpClient.Builder().build();
            var response = client.newCall(request).execute();
            if (response.code() == expectedStatusCode) {
                return;
            }
            throw new ClientException(response.code());
        } catch (IOException e) {
            log.warn("Unexpected error when making request", e);
            throw new ClientException(0);
        }
    }
}
