package com.amcglynn.myzappi.core.model;

import lombok.Getter;

@Getter
public enum AutomationPredicateType {
    ENERGY_SOLAR_GENERATION_KW(null),
    ENERGY_EXPORTING_KW(null),
    ENERGY_IMPORTING_KW(null),
    ENERGY_CONSUMING_KW(null),
    ZAPPI_EV_CHARGE_RATE_KW(DeviceClass.ZAPPI),
    LIBBI_STATE_OF_CHARGE_PERCENT(DeviceClass.LIBBI);

    private final DeviceClass deviceClass;

    AutomationPredicateType(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

    public boolean requiresTarget() {
        return deviceClass != null;
    }
}
