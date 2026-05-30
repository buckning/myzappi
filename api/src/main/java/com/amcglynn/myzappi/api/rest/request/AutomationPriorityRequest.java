package com.amcglynn.myzappi.api.rest.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AutomationPriorityRequest {
    private List<String> automationIds;
}
