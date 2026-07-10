package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myzappi.core.exception.AutomationValidationException;
import com.amcglynn.myzappi.core.model.AutomationOperator;
import com.amcglynn.myzappi.core.model.AutomationPredicate;
import com.amcglynn.myzappi.core.model.AutomationPredicateType;
import com.amcglynn.myzappi.core.model.AutomationSnapshot;
import com.amcglynn.myzappi.core.model.SerialNumber;

import java.math.BigDecimal;

public class PredicateEvaluator {

    public boolean evaluate(AutomationPredicate predicate, AutomationSnapshot snapshot) {
        return evaluateWithResult(predicate, snapshot).conditionMet();
    }

    public PredicateEvaluationResult evaluateWithResult(AutomationPredicate predicate, AutomationSnapshot snapshot) {
        var actual = actualValue(predicate, snapshot);
        var expected = new BigDecimal(predicate.getValue());
        var comparison = actual.compareTo(expected);
        if (predicate.getOperator() == AutomationOperator.GREATER_THAN) {
            return new PredicateEvaluationResult(actual, expected, predicate.getOperator(), comparison > 0);
        }
        if (predicate.getOperator() == AutomationOperator.LESS_THAN) {
            return new PredicateEvaluationResult(actual, expected, predicate.getOperator(), comparison < 0);
        }
        throw new AutomationValidationException("Unsupported automation operator " + predicate.getOperator());
    }

    private BigDecimal actualValue(AutomationPredicate predicate, AutomationSnapshot snapshot) {
        var type = AutomationPredicateType.valueOf(predicate.getType());
        return switch (type) {
            case ENERGY_SOLAR_GENERATION_KW ->
                    BigDecimal.valueOf(snapshot.getEnergyStatus().getSolarGenerationKW().getDouble());
            case ENERGY_EXPORTING_KW -> BigDecimal.valueOf(snapshot.getEnergyStatus().getExportingKW().getDouble());
            case ENERGY_IMPORTING_KW -> BigDecimal.valueOf(snapshot.getEnergyStatus().getImportingKW().getDouble());
            case ENERGY_CONSUMING_KW -> BigDecimal.valueOf(snapshot.getEnergyStatus().getConsumingKW().getDouble());
            case ZAPPI_EV_CHARGE_RATE_KW -> BigDecimal.valueOf(snapshot
                    .getZappiEvChargeRateKW(SerialNumber.from(predicate.getTarget().orElseThrow())).getDouble());
            case LIBBI_STATE_OF_CHARGE_PERCENT -> BigDecimal.valueOf(snapshot
                    .getLibbiStateOfChargePercent(SerialNumber.from(predicate.getTarget().orElseThrow()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No Libbi state of charge found for " + predicate.getTarget().orElse(""))));
        };
    }
}
