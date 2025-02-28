package com.amcglynn.myenergi;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum LibbiState {
    // from https://github.com/CJNE/pymyenergi/pull/16/files
    OFF(0, "Off"),
    ON(1, "On"),
    BATTERY_FULL(2, "Full"),
    IDLE(4, "Idle"),
    CHARGING(5, "Charging"),
    DISCHARGING(6, "Discharging"),
    DURATION_CHARGING(7, "Charging"),
    DURATION_DRAIN(8, "Discharging"),
    TARGET_CHARGE(12, "Charging"),
    BOOSTING(51, "Charging"),
    BOOSTING2(53, "Charging"),
    BOOSTING3(55, "Charging"),
    STOPPED(11, "Stopped"),
    BATTERY_EMPTY(101, "Empty"),
    FULL(102, "Full"),
    FULL2(104, "Full"),
    FW_UPGRADE_ARM(151, "Upgrading"),
    FW_UPGRADE_DSP(156, "Upgrading"),
    BMS_CHARGE_TEMPERATURE_LOW(172, "Stopped"),
    CALIBRATION_CHARGE(234, "Calibrating"),
    FW_UPGRADE_DSP2(251, "Upgrading"),
    FW_UPGRADE_ARM2(252, "Upgrading"),
    UNKNOWN(-1, "Unknown");

    private final int state;
    @Getter
    private final String description;

    private static final Map<Integer, LibbiState> CODES = new HashMap<>();

    static {
        for (LibbiState libbiState : values()) {
            CODES.put(libbiState.state, libbiState);
        }
    }

    LibbiState(int state, String description) {
        this.state = state;
        this.description = description;
    }

    public static LibbiState from(int i) {
        var status = CODES.get(i);
        if (status == null) {
            status = UNKNOWN;
        }
        return status;
    }
}
