package com.amcglynn.sqs;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class MyZappiReminderEvent {
    private String type;
    private String alexaBaseUrl;
    private String alexaUserId;

    public MyZappiReminderEvent(Map<String, String> input) {
        type = getOrThrow(input, "type");
        alexaBaseUrl = getOrThrow(input, "alexaBaseUrl");
        alexaUserId = getOrThrow(input, "alexaUserId");
    }

    private String getOrThrow(Map<String, String> input, String key) {
        var value = input.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Key " + key + " not found");
        }
        return value;
    }
}
