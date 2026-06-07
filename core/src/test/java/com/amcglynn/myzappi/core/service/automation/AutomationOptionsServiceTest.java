package com.amcglynn.myzappi.core.service.automation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutomationOptionsServiceTest {

    @Test
    void optionsContainAllSupportedPredicatesOperatorsAndActions() {
        var options = new AutomationOptionsService().getOptions();

        assertThat(options.getOperators()).containsExactlyInAnyOrder("GREATER_THAN", "LESS_THAN");
        assertThat(options.getPredicates()).extracting("type")
                .containsExactlyInAnyOrder("ENERGY_SOLAR_GENERATION_KW", "ENERGY_EXPORTING_KW",
                        "ENERGY_IMPORTING_KW", "ENERGY_CONSUMING_KW", "ZAPPI_EV_CHARGE_RATE_KW",
                        "LIBBI_STATE_OF_CHARGE_PERCENT");
        assertThat(options.getActions()).extracting("type")
                .containsExactlyInAnyOrder("setChargeMode", "setZappiMgl", "setEddiMode", "setLibbiEnabled",
                        "setLibbiChargeFromGrid", "setLibbiChargeTarget");
    }
}
