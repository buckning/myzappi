package com.amcglynn.myzappi.login.rest.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ScheduleAction {

    private String type;
    private String value;
}
