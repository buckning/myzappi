package com.amcglynn.lwa;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LwaClientTest {

    @Test
    void testGetUserId() {
        var mockWebServer = new MockWebServer();

        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody("{\n" +
                        "    \"user_id\": \"userId\",\n" +
                        "    \"name\": \"Test User\",\n" +
                        "    \"email\": \"user@test.com\"\n" +
                        "}");
        mockWebServer.enqueue(mockResponse);

        var client = new LwaClient();
        client.setGetProfileUrl(mockWebServer.url("").toString());
        assertThat(client.getUserId("myAccessToken")).contains("userId");
    }

    @Test
    void testGetUserProfile() {
        var mockWebServer = new MockWebServer();

        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody("{\n" +
                        "    \"user_id\": \"userId\",\n" +
                        "    \"name\": \"Test User\",\n" +
                        "    \"email\": \"user@test.com\"\n" +
                        "}");
        mockWebServer.enqueue(mockResponse);

        var client = new LwaClient();
        client.setGetProfileUrl(mockWebServer.url("").toString());
        var response = client.getUserProfile("myAccessToken");
        assertThat(response).isPresent();
        assertThat(response.get().getUserId()).isEqualTo("userId");
        assertThat(response.get().getName()).isEqualTo("Test User");
        assertThat(response.get().getEmail()).isEqualTo("user@test.com");
    }

    @Test
    void testGetUserProfileReturnsEmptyOptionalIfAccessTokenIsInvalid() {
        var mockWebServer = new MockWebServer();

        var mockResponse = new MockResponse()
                .setResponseCode(400)
                .setBody("{\n" +
                        "    \"error_description\": \"The request has an invalid parameter : access_token\",\n" +
                        "    \"error\": \"invalid_token\"\n" +
                        "}");
        mockWebServer.enqueue(mockResponse);

        var client = new LwaClient();
        client.setGetProfileUrl(mockWebServer.url("").toString());
        assertThat(client.getUserProfile("myAccessToken")).isEmpty();
    }


    @Test
    void testGetUserProfileReturnsEmptyOptionalWhenItCannotCommunicateWithTheServer() throws Exception {
        var mockWebServer = new MockWebServer();

        var client = new LwaClient();
        client.setGetProfileUrl(mockWebServer.url("").toString());
        mockWebServer.shutdown();
        assertThat(client.getUserProfile("myAccessToken")).isEmpty();
    }

    @Test
    void testGetTokenInfo() {
        var mockWebServer = new MockWebServer();

        var mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody("{\n" +
                        "    \"aud\": \"myclientid\",\n" +
                        "    \"user_id\": \"userId\",\n" +
                        "    \"iss\": \"https://www.amazon.com\",\n" +
                        "    \"exp\": 3600,\n" +
                        "    \"app_id\": \"myappid\",\n" +
                        "    \"iat\": 1686988653\n" +
                        "}");
        mockWebServer.enqueue(mockResponse);

        var client = new LwaClient();
        client.setGetTokenInfoUrl(mockWebServer.url("").toString());
        var response = client.getTokenInfo("myAccessToken");
        assertThat(response).isPresent();
        assertThat(response.get().getUserId()).isEqualTo("userId");
        assertThat(response.get().getAudienceClaim()).isEqualTo("myclientid");
        assertThat(response.get().getIssuer()).isEqualTo("https://www.amazon.com");
        assertThat(response.get().getAppId()).isEqualTo("myappid");
        assertThat(response.get().getIssuedAt()).isEqualTo(1686988653L);
        assertThat(response.get().getExpires()).isEqualTo(3600L);
    }

    @Test
    void testGetTokenInfoReturnsEmptyOptionalIfAccessTokenIsInvalid() {
        var mockWebServer = new MockWebServer();

        var mockResponse = new MockResponse()
                .setResponseCode(400)
                .setBody("{\n" +
                        "    \"error_index\": \"dGhpc2lzZHVtbXlkYXRh\",\n" +
                        "    \"error_description\": \"The request has an invalid parameter : access_token\",\n" +
                        "    \"error\": \"invalid_token\"\n" +
                        "}");
        mockWebServer.enqueue(mockResponse);

        var client = new LwaClient();
        client.setGetTokenInfoUrl(mockWebServer.url("").toString());
        assertThat(client.getUserProfile("myAccessToken")).isEmpty();
    }


    @Test
    void testGetTokenInfoReturnsEmptyOptionalWhenItCannotCommunicateWithTheServer() throws Exception {
        var mockWebServer = new MockWebServer();

        var client = new LwaClient();
        client.setGetTokenInfoUrl(mockWebServer.url("").toString());
        mockWebServer.shutdown();
        assertThat(client.getTokenInfo("myAccessToken")).isEmpty();
    }
}
