package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Schedule {
    private String id;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime startDateTime;

    @JsonDeserialize(using = ZoneIdDeserializer.class)
    @JsonSerialize(using = ZoneIdSerializer.class)
    private ZoneId zoneId;

    private ScheduleRecurrence recurrence;

    private ScheduleAction action;

    @Setter
    private Boolean active;

    public boolean isActive() {
        return active == null || active;
    }
}
