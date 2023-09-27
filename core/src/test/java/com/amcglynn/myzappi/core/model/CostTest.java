package com.amcglynn.myzappi.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CostTest {

    @Test
    void testCostSingleUnit() {
        var cost = new Cost("GBP", 5);
        assertThat(cost.getSubUnitValue()).isEqualTo(0);
        assertThat(cost.getBaseCurrencyValue()).isEqualTo(5);
        assertThat(cost.getBaseCurrency()).isEqualTo("Pounds");
        assertThat(cost.getSubUnit()).isEqualTo("pence");
    }

    @Test
    void testCostPluralUnit() {
        var cost = new Cost("GBP", 1);
        assertThat(cost.getSubUnitValue()).isEqualTo(0);
        assertThat(cost.getBaseCurrencyValue()).isEqualTo(1);
        assertThat(cost.getBaseCurrency()).isEqualTo("Pound");
        assertThat(cost.getSubUnit()).isEqualTo("pence");
    }

    @Test
    void testCost() {
        var cost = new Cost("EUR", 1.27);
        assertThat(cost.getSubUnitValue()).isEqualTo(27);
        assertThat(cost.getBaseCurrencyValue()).isEqualTo(1);
    }

    @Test
    void testNegativeCost() {
        var cost = new Cost("EUR", -1.27);
        assertThat(cost.getSubUnitValue()).isEqualTo(27);
        assertThat(cost.getBaseCurrencyValue()).isEqualTo(-1);
    }
}
