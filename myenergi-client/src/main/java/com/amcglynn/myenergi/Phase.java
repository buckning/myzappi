package com.amcglynn.myenergi;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum Phase {
    // Single phase (1), the charge rate is 7.3kW
    // 3 phase (3), the charge rate is 22kW
    // https://support.myenergi.com/hc/en-gb/articles/5780558509201-ECO-ECO-charge-rates-in-a-three-phase-zappi

    SINGLE(1, 7.3),
    THREE_PHASE(3, 22.0);

    private static final Map<Integer, Phase> CODES = new HashMap<>();

    static {
        for (Phase status : values()) {
            CODES.put(status.code, status);
        }
    }

    @Getter
    private int code;
    @Getter
    private double maxChargeRate;

    Phase(int code, double maxChargeRate) {
        this.code = code;
        this.maxChargeRate = maxChargeRate;
    }

    public static Phase from(int i) {
        var phase = CODES.get(i);
        if (phase == null) {
            phase = SINGLE;
        }
        return phase;
    }
}
