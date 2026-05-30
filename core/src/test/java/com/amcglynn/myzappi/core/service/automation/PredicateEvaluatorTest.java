package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myenergi.units.KiloWatt;
import com.amcglynn.myenergi.units.Watt;
import com.amcglynn.myzappi.core.model.AutomationOperator;
import com.amcglynn.myzappi.core.model.AutomationPredicate;
import com.amcglynn.myzappi.core.model.AutomationSnapshot;
import com.amcglynn.myzappi.core.model.EnergyStatus;
import com.amcglynn.myzappi.core.model.SerialNumber;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PredicateEvaluatorTest {

    private final PredicateEvaluator evaluator = new PredicateEvaluator();
    private final SerialNumber zappiSerialNumber = SerialNumber.from("10000001");
    private final SerialNumber libbiSerialNumber = SerialNumber.from("30000001");

    @Test
    void evaluatesSolarGenerationGreaterThan() {
        assertThat(evaluator.evaluate(predicate("ENERGY_SOLAR_GENERATION_KW", "4.9"), snapshot())).isTrue();
    }

    @Test
    void evaluatesExportingLessThan() {
        assertThat(evaluator.evaluate(predicate("ENERGY_EXPORTING_KW", AutomationOperator.LESS_THAN, "2.0"), snapshot())).isTrue();
    }

    @Test
    void evaluatesImportingGreaterThan() {
        assertThat(evaluator.evaluate(predicate("ENERGY_IMPORTING_KW", "0.4"), snapshot())).isTrue();
    }

    @Test
    void evaluatesConsumingLessThan() {
        assertThat(evaluator.evaluate(predicate("ENERGY_CONSUMING_KW", AutomationOperator.LESS_THAN, "3.7"), snapshot())).isTrue();
    }

    @Test
    void evaluatesZappiEvChargeRateGreaterThanForTarget() {
        var predicate = predicate("ZAPPI_EV_CHARGE_RATE_KW", "3.1").toBuilder()
                .target(zappiSerialNumber.toString())
                .build();

        assertThat(evaluator.evaluate(predicate, snapshot())).isTrue();
    }

    @Test
    void evaluatesLibbiStateOfChargeLessThanForTarget() {
        var predicate = predicate("LIBBI_STATE_OF_CHARGE_PERCENT", AutomationOperator.LESS_THAN, "82")
                .toBuilder()
                .target(libbiSerialNumber.toString())
                .build();

        assertThat(evaluator.evaluate(predicate, snapshot())).isTrue();
    }

    @Test
    void throwsWhenPredicateValueIsNotDecimal() {
        assertThatThrownBy(() -> evaluator.evaluate(predicate("ENERGY_EXPORTING_KW", "not-decimal"), snapshot()))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void throwsWhenTargetedSnapshotValueIsMissing() {
        var predicate = predicate("LIBBI_STATE_OF_CHARGE_PERCENT", AutomationOperator.LESS_THAN, "82")
                .toBuilder()
                .target("99999999")
                .build();

        assertThatThrownBy(() -> evaluator.evaluate(predicate, snapshot()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AutomationPredicate predicate(String type, String value) {
        return predicate(type, AutomationOperator.GREATER_THAN, value);
    }

    private AutomationPredicate predicate(String type, AutomationOperator operator, String value) {
        return AutomationPredicate.builder()
                .type(type)
                .operator(operator)
                .value(value)
                .build();
    }

    private AutomationSnapshot snapshot() {
        return AutomationSnapshot.builder()
                .energyStatus(EnergyStatus.builder()
                        .solarGenerationKW(new KiloWatt(new Watt(5000L)))
                        .exportingKW(new KiloWatt(new Watt(1500L)))
                        .importingKW(new KiloWatt(new Watt(500L)))
                        .consumingKW(new KiloWatt(new Watt(3500L)))
                        .build())
                .zappiEvChargeRateKWBySerialNumber(Map.of(zappiSerialNumber, new KiloWatt(new Watt(3200L))))
                .libbiStateOfChargePercentBySerialNumber(Map.of(libbiSerialNumber, 81))
                .build();
    }
}
