package com.amcglynn.myzappi.api.rest.response;

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
public class ScheduleResponse {
    private List<Schedule> schedules;
}
