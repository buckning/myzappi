package com.amcglynn.myzappi.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ScheduleDetails {
    private String scheduleId;
    private UserId lwaUserId;
}
