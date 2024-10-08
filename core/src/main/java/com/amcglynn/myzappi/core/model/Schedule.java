package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
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
}
