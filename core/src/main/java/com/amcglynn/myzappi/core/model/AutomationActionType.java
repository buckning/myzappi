package com.amcglynn.myzappi.core.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum AutomationActionType {
    SET_CHARGE_MODE("setChargeMode", DeviceClass.ZAPPI),
    SET_ZAPPI_MGL("setZappiMgl", DeviceClass.ZAPPI),
    SET_EDDI_MODE("setEddiMode", DeviceClass.EDDI),
    SET_LIBBI_ENABLED("setLibbiEnabled", DeviceClass.LIBBI),
    SET_LIBBI_CHARGE_FROM_GRID("setLibbiChargeFromGrid", DeviceClass.LIBBI),
    SET_LIBBI_CHARGE_TARGET("setLibbiChargeTarget", DeviceClass.LIBBI);

    private final String type;
    private final DeviceClass deviceClass;

    AutomationActionType(String type, DeviceClass deviceClass) {
        this.type = type;
        this.deviceClass = deviceClass;
    }

    public static Optional<AutomationActionType> from(String type) {
        return Arrays.stream(values())
                .filter(value -> value.type.equals(type))
                .findFirst();
    }

    public static Set<String> names() {
        return Arrays.stream(values())
                .map(AutomationActionType::getType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
