package com.amcglynn.sqs;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class MyZappiScheduleEvent {
    private String alexaBaseUrl;
    private String alexaUserId;
    private String scheduleId;
    private String lwaUserId;

    public MyZappiScheduleEvent(Map<String, String> input) {
        alexaUserId = input.get("alexaUserId");
        lwaUserId = input.get("lwaUserId");

        if (alexaUserId == null && lwaUserId == null) {
            throw new IllegalArgumentException("lwaUserId and alexaUserId are both null");
        }

        if (alexaUserId != null) {
            alexaBaseUrl = getOrThrow(input, "alexaBaseUrl");
        }

        if (lwaUserId != null) {
            scheduleId = getOrThrow(input, "scheduleId");
        }
    }

    public boolean isAlexaReminder() {
        return alexaUserId != null;
    }

    private String getOrThrow(Map<String, String> input, String key) {
        var value = input.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Key " + key + " not found");
        }
        return value;
    }
}
