package com.amcglynn.myzappi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CostTest {

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
