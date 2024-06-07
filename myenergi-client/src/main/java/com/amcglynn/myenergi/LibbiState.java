package com.amcglynn.myenergi;

import java.util.HashMap;
import java.util.Map;

public enum LibbiState {
    // from https://github.com/CJNE/pymyenergi/pull/16/files
    OFF(0),
    ON(1),
    BATTERY_FULL(2),
    IDLE(4),
    CHARGING(5),
    DISCHARGING(6),
    DURATION_CHARGING(7),
    DURATION_DRAIN(8),
    TARGET_CHARGE(12),
    BOOSTING(51),
    BOOSTING2(53),
    BOOSTING3(55),
    STOPPED(11),
    BATTERY_EMPTY(101),
    FULL(102),
    FULL2(104),
    FW_UPGRADE_ARM(151),
    FW_UPGRADE_DSP(156),
    BMS_CHARGE_TEMPERATURE_LOW(172),
    CALIBRATION_CHARGE(234),
    FW_UPGRADE_DSP2(251),
    FW_UPGRADE_ARM2(252),
    UNKNOWN(-1);

    private final int state;

    private static final Map<Integer, LibbiState> CODES = new HashMap<>();

    static {
        for (LibbiState libbiState : values()) {
            CODES.put(libbiState.state, libbiState);
        }
    }

    public int getStateValue() {
        return this.state;
    }

    LibbiState(int state) {
        this.state = state;
    }

    public static LibbiState from(int i) {
        var status = CODES.get(i);
        if (status == null) {
            status = UNKNOWN;
        }
        return status;
    }
}
