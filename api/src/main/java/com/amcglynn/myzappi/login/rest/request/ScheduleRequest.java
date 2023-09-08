package com.amcglynn.myzappi.login.rest.request;

import com.amcglynn.myzappi.core.model.Schedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ScheduleRequest {
    private List<Schedule> schedules;
}
