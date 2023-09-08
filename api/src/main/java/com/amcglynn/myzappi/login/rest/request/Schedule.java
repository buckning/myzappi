package com.amcglynn.myzappi.login.rest.request;

import com.amcglynn.myzappi.core.model.LocalTimeDeserializer;
import com.amcglynn.myzappi.core.model.LocalTimeSerializer;
import com.amcglynn.myzappi.core.model.ZoneIdDeserializer;
import com.amcglynn.myzappi.login.model.ScheduleType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Schedule {
    private String id;
    private String type;

    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime startTime;

    @JsonDeserialize(using = ZoneIdDeserializer.class)
    private ZoneId zoneId;

    private List<Integer> days;

    private ScheduleAction action;
}
