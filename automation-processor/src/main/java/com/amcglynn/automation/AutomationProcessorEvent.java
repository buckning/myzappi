package com.amcglynn.automation;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class AutomationProcessorEvent {
    private String runId;
    private Map<String, AttributeValue> lastEvaluatedKey;
}
