package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AutomationModelTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

    @Test
    void activeDefaultsToTrueWhenMissing() throws Exception {
        var automation = objectMapper.readValue("""
                {"automationId":"a-1","priority":1,"predicate":{"type":"ENERGY_EXPORTING_KW","operator":"GREATER_THAN","value":"2.0"},"action":{"type":"setChargeMode","target":"10000001","value":"ECO_PLUS"}}
                """, Automation.class);

        assertThat(automation.isActive()).isTrue();
    }

    @Test
    void actionTargetIsOptionalAtModelLevelSoValidatorOwnsTargetRules() {
        var action = AutomationAction.builder().type("setChargeMode").value("ECO_PLUS").build();

        assertThat(action.getTarget()).isEmpty();
    }

    @Test
    void catalogsExposeOnlyV1PredicatesAndActions() {
        assertThat(AutomationPredicateType.values()).extracting(AutomationPredicateType::name)
                .containsExactlyInAnyOrder(
                        "ENERGY_SOLAR_GENERATION_KW",
                        "ENERGY_EXPORTING_KW",
                        "ENERGY_IMPORTING_KW",
                        "ENERGY_CONSUMING_KW",
                        "ZAPPI_EV_CHARGE_RATE_KW",
                        "LIBBI_STATE_OF_CHARGE_PERCENT");
        assertThat(AutomationActionType.names())
                .containsExactlyInAnyOrder("setChargeMode", "setZappiMgl", "setEddiMode",
                        "setLibbiEnabled", "setLibbiChargeFromGrid", "setLibbiChargeTarget");
    }

    @Test
    void stateEntryCanRecordProcessorRuntimeFields() {
        var now = LocalDateTime.of(2026, 5, 30, 12, 0);

        var state = AutomationStateEntry.builder()
                .lastPredicateMatched(true)
                .lastEvaluatedAt(now)
                .lastTriggeredAt(now)
                .lastError("action failed")
                .lastFailedAt(now)
                .lastSkippedReason("conflict")
                .build();

        assertThat(state.getLastPredicateMatched()).isTrue();
        assertThat(state.getLastSkippedReason()).isEqualTo("conflict");
    }
}
