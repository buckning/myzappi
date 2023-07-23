package com.amcglynn.myenergi;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum LockStatus {
    // Bit 0: Locked Now
    // Bit 1: Lock when plugged in
    // Bit 2: Lock when unplugged.
    // Bit 3: Charge when locked.
    // Bit 4: Charge Session Allowed (Even if locked)

    // with some testing, two types of common scenarios are
    // "lck": 7 - it is locked but cable is plugged in.
    // "lck": 23 - it is locked and waiting for charge.

    // If the zappi is locked when you plug in the car then Bit 4 is 0
    // When you enter the PIN on the zappi or unlock it from the (next) version of the app it's 1
    // https://myenergi.info/lck-value-in-zappi-json-t2108.html

    CHARGE_ALLOWED(23),
    LOCKED(7),
    UNKNOWN(-1);

    private static final Map<Integer, LockStatus> CODES = new HashMap<>();

    static {
        for (LockStatus status : values()) {
            CODES.put(status.code, status);
        }
    }

    @Getter
    private int code;

    LockStatus(int code) {
        this.code = code;
    }

    public static LockStatus from(int i) {
        var status = CODES.get(i);
        if (status == null) {
            status = UNKNOWN;
        }
        return status;
    }
}
