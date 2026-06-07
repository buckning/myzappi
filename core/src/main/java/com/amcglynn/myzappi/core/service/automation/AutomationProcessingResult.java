package com.amcglynn.myzappi.core.service.automation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class AutomationProcessingResult {
    private int evaluated;
    private int triggered;
    private int executed;
    private int skipped;
    private int failed;
}
