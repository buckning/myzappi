package com.amcglynn.lwa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * ASK SDK does not deserialize the timestamps correctly and throws an exception when getting reminders, so this is done as a workaround
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Reminder {

    private String alertToken;
    private String createdTime;
    private String updatedTime;
    private Trigger trigger;
    private AlertInfo alertInfo;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Builder
    @AllArgsConstructor
    @Getter
    public static class Trigger {
        String type;
        String scheduledTime;
        String timeZoneId;
        int offsetInSeconds;
        Recurrence recurrence;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Builder
    @AllArgsConstructor
    @Getter
    public static class Recurrence {
        String freq;
        String startDateTime;
        String endDateTime;
        List<String> recurrenceRules;

        public ZonedDateTime getStartTime() {
            return ZonedDateTime.parse(startDateTime);
        }

        public ZonedDateTime getEndTime() {
            return ZonedDateTime.parse(endDateTime);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Builder
    @AllArgsConstructor
    @Getter
    public static class AlertInfo {
        SpokenInfo spokenInfo;

        @JsonIgnoreProperties(ignoreUnknown = true)
        @NoArgsConstructor
        @Builder
        @AllArgsConstructor
        @Getter
        public static class SpokenInfo {
            List<Content> content;

            @JsonIgnoreProperties(ignoreUnknown = true)
            @NoArgsConstructor
            @Builder
            @AllArgsConstructor
            @Getter
            public static class Content {
                String locale;
                String text;
            }
        }
    }
}
