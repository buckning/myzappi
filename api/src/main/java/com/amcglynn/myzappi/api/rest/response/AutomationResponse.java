package com.amcglynn.myzappi.api.rest.response;

import com.amcglynn.myzappi.core.model.Automation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class AutomationResponse {
    private List<Automation> automations;
}
