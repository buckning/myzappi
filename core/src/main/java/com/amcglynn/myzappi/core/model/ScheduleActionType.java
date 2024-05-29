package com.amcglynn.myzappi.core.model;

import lombok.Getter;

import java.util.List;

public enum ScheduleActionType {
    SET_ZAPPI_CHARGE_MODE("setChargeMode", DeviceClass.ZAPPI),
    SET_ZAPPI_BOOST_KWH("setBoostKwh", DeviceClass.ZAPPI),
    SET_ZAPPI_BOOST_UNTIL("setBoostUntil", DeviceClass.ZAPPI),
    SET_ZAPPI_BOOST_FOR("setBoostFor", DeviceClass.ZAPPI),
    SET_EDDI_MODE("setEddiMode", DeviceClass.EDDI),
    SET_EDDI_BOOST_FOR("setEddiBoostFor", DeviceClass.EDDI),
    SET_LIBBI_ENABLED("setLibbiEnabled", DeviceClass.LIBBI),
    SET_LIBBI_CHARGE_FROM_GRID("setLibbiChargeFromGrid", DeviceClass.LIBBI),
    SET_LIBBI_CHARGE_TARGET("setLibbiChargeTarget", DeviceClass.LIBBI);

    private final String name;
    @Getter
    private final DeviceClass deviceClass;

    ScheduleActionType(String name, DeviceClass deviceClass) {
        this.name = name;
        this.deviceClass = deviceClass;
    }

    public static ScheduleActionType from(String scheduleActionType) {
        return List.of(ScheduleActionType.values())
                .stream()
                .filter(type -> type.name.equals(scheduleActionType))
                .findFirst()
                .orElse(null);
    }
}
