package com.amcglynn.myzappi.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleActionTypeTest {

    @Test
    void from() {
        assertThat(ScheduleActionType.from("setChargeMode")).isEqualTo(ScheduleActionType.SET_ZAPPI_CHARGE_MODE);
    }

    @Test
    void fromReturnsNullWhenTypeNotFound() {
        assertThat(ScheduleActionType.from("setChargdddeMode")).isNull();
    }
}
