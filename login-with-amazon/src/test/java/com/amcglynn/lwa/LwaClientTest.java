package com.amcglynn.lwa;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LwaClientTest {

    @Test
    void testGetUserId() throws Exception {
        try (var mockWebServer = new MockWebServer()) {
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
    }

    @Test
    void testGetUserProfile() throws Exception {
        try (var mockWebServer = new MockWebServer()) {
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
    }

    @Test
    void testGetUserProfileReturnsEmptyOptionalIfAccessTokenIsInvalid() throws Exception {
        try (var mockWebServer = new MockWebServer()) {
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
    }


    @Test
    void testGetUserProfileReturnsEmptyOptionalWhenItCannotCommunicateWithTheServer() throws Exception {
        try (var mockWebServer = new MockWebServer()) {
            var client = new LwaClient();
            client.setGetProfileUrl(mockWebServer.url("").toString());
            mockWebServer.shutdown();
            assertThat(client.getUserProfile("myAccessToken")).isEmpty();
        }
    }

    @Test
    void testGetTokenInfo() throws Exception {
        try (var mockWebServer = new MockWebServer()) {
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
    }

    @Test
    void testGetTimeZone() throws Exception {
        try (var mockWebServer = new MockWebServer()) {
            var mockResponse = new MockResponse()
                    .setResponseCode(200)
                    .setBody("\"Europe/Dublin\"");
            mockWebServer.enqueue(mockResponse);

            var client = new LwaClient();
            var url = mockWebServer.url("").toString();
            var response = client.getTimeZone(url, "myDeviceId", "myAccessToken");
            assertThat(response).isPresent();
            assertThat(response.get()).contains("Europe/Dublin");
        }
    }

    @Test
    void testGetMessagingToken() throws Exception {
        try (var mockWebServer = new MockWebServer()) {
            var mockResponse = new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\n" +
                            "    \"access_token\": \"testAccessToken\",\n" +
                            "    \"scope\": \"alexa:skill_messaging\",\n" +
                            "    \"token_type\": \"bearer\",\n" +
                            "    \"expires_in\": 3600\n" +
                            "}");
            mockWebServer.enqueue(mockResponse);

            var client = new LwaClient();
            client.setGetTokenUrl(mockWebServer.url("").toString());
            var response = client.getMessagingToken( "myClientId", "myClientSecret");
            assertThat(response.getAccessToken()).isEqualTo("testAccessToken");
            assertThat(response.getScope()).isEqualTo("alexa:skill_messaging");
            assertThat(response.getTokenType()).isEqualTo("bearer");
            assertThat(response.getExpiresIn()).isEqualTo(3600L);
        }
    }

    @Test
    void testPostSkillMessage() throws Exception {
        try (var mockWebServer = new MockWebServer()) {

            var mockResponse = new MockResponse()
                    .setResponseCode(202);
            mockWebServer.enqueue(mockResponse);

            var client = new LwaClient();
            var throwable = catchThrowable(() -> client.postSkillMessage(mockWebServer.url("").toString(),
                    "mockUser", "mockAccessToken", new ReminderUpdate("test", "test", "test")));
            assertThat(throwable).isNull();
        }
    }

    @Test
    void testGetReminders() throws Exception {
        try (var mockWebServer = new MockWebServer()) {

            var mockResponse = new MockResponse()
                    .setBody("{\n" +
                            "    \"totalCount\": \"1\",\n" +
                            "    \"alerts\": [\n" +
                            "        {\n" +
                            "            \"alertToken\": \"d8337972-23eb-4036-a83d-4a35bc9a0104\",\n" +
                            "            \"createdTime\": \"2023-08-26T21:23:54.205Z\",\n" +
                            "            \"updatedTime\": \"2023-08-26T21:23:54.472Z\",\n" +
                            "            \"trigger\": {\n" +
                            "                \"type\": \"SCHEDULED_ABSOLUTE\",\n" +
                            "                \"scheduledTime\": \"2023-08-27T18:58:00.000\",\n" +
                            "                \"timeZoneId\": \"Europe/Dublin\",\n" +
                            "                \"offsetInSeconds\": 0,\n" +
                            "                \"recurrence\": {\n" +
                            "                    \"freq\": null,\n" +
                            "                    \"byDay\": null,\n" +
                            "                    \"startDateTime\": \"2023-08-27T00:00:00.000+01:00\",\n" +
                            "                    \"endDateTime\": \"2023-09-01T00:00:00.000+01:00\",\n" +
                            "                    \"recurrenceRules\": [\n" +
                            "                        \"FREQ=DAILY;BYHOUR=18;BYMINUTE=58;BYSECOND=0\"\n" +
                            "                    ]\n" +
                            "                },\n" +
                            "                \"eventTime\": null\n" +
                            "            },\n" +
                            "            \"status\": \"ON\",\n" +
                            "            \"alertInfo\": {\n" +
                            "                \"spokenInfo\": {\n" +
                            "                    \"content\": [\n" +
                            "                        {\n" +
                            "                            \"locale\": \"en-GB\",\n" +
                            "                            \"text\": \"Your car is not plugged in\",\n" +
                            "                            \"ssml\": null\n" +
                            "                        }\n" +
                            "                    ]\n" +
                            "                }\n" +
                            "            },\n" +
                            "            \"pushNotification\": {\n" +
                            "                \"status\": \"ENABLED\"\n" +
                            "            },\n" +
                            "            \"version\": \"6\"\n" +
                            "        }\n" +
                            "    ],\n" +
                            "    \"links\": null\n" +
                            "}")
                    .setResponseCode(200);
            mockWebServer.enqueue(mockResponse);

            var client = new LwaClient();
            var reminders = client.getReminders(mockWebServer.url("").toString(), "mockAccessToken");
            assertThat(reminders).isNotNull();
        }
    }

    @Test
    void testGetReminder() throws Exception {
        try (var mockWebServer = new MockWebServer()) {

            var mockResponse = new MockResponse()
                    .setBody(
                            "        {\n" +
                            "            \"alertToken\": \"d8337972-23eb-4036-a83d-4a35bc9a0104\",\n" +
                            "            \"createdTime\": \"2023-08-26T21:23:54.205Z\",\n" +
                            "            \"updatedTime\": \"2023-08-26T21:23:54.472Z\",\n" +
                            "            \"trigger\": {\n" +
                            "                \"type\": \"SCHEDULED_ABSOLUTE\",\n" +
                            "                \"scheduledTime\": \"2023-08-27T18:58:00.000\",\n" +
                            "                \"timeZoneId\": \"Europe/Dublin\",\n" +
                            "                \"offsetInSeconds\": 0,\n" +
                            "                \"recurrence\": {\n" +
                            "                    \"freq\": null,\n" +
                            "                    \"byDay\": null,\n" +
                            "                    \"startDateTime\": \"2023-08-27T00:00:00.000+01:00\",\n" +
                            "                    \"endDateTime\": \"2023-09-01T00:00:00.000+01:00\",\n" +
                            "                    \"recurrenceRules\": [\n" +
                            "                        \"FREQ=DAILY;BYHOUR=18;BYMINUTE=58;BYSECOND=0\"\n" +
                            "                    ]\n" +
                            "                },\n" +
                            "                \"eventTime\": null\n" +
                            "            },\n" +
                            "            \"status\": \"ON\",\n" +
                            "            \"alertInfo\": {\n" +
                            "                \"spokenInfo\": {\n" +
                            "                    \"content\": [\n" +
                            "                        {\n" +
                            "                            \"locale\": \"en-GB\",\n" +
                            "                            \"text\": \"Your car is not plugged in\",\n" +
                            "                            \"ssml\": null\n" +
                            "                        }\n" +
                            "                    ]\n" +
                            "                }\n" +
                            "            },\n" +
                            "            \"pushNotification\": {\n" +
                            "                \"status\": \"ENABLED\"\n" +
                            "            },\n" +
                            "            \"version\": \"6\"\n" +
                            "        }\n")
                    .setResponseCode(200);
            mockWebServer.enqueue(mockResponse);

            var client = new LwaClient();
            var reminder = client.getReminder(mockWebServer.url("").toString(), "mockAccessToken", "d8337972-23eb-4036-a83d-4a35bc9a0104");
            assertThat(reminder).isNotNull();
        }
    }

    @Test
    void testGetTokenInfoReturnsEmptyOptionalIfAccessTokenIsInvalid() throws Exception {
        try (var mockWebServer = new MockWebServer()) {
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
    }


    @Test
    void testGetTokenInfoReturnsEmptyOptionalWhenItCannotCommunicateWithTheServer() throws Exception {
        try (var mockWebServer = new MockWebServer()) {
            var client = new LwaClient();
            client.setGetTokenInfoUrl(mockWebServer.url("").toString());
            mockWebServer.shutdown();
            assertThat(client.getTokenInfo("myAccessToken")).isEmpty();
        }
    }
}
