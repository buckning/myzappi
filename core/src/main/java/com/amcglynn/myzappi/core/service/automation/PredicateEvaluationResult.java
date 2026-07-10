package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myzappi.core.model.AutomationOperator;

import java.math.BigDecimal;

public record PredicateEvaluationResult(BigDecimal deviceValue,
                                        BigDecimal configuredValue,
                                        AutomationOperator operator,
                                        boolean conditionMet) {
}
