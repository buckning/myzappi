package com.amcglynn.myenergi;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum EddiState {
    PAUSED(1, "Paused"),
    DIVERTING(3, "Diverting"),
    BOOST(4, "Boost"),
    MAX_TEMP_REACHED(5, "Max Temp Reached"),
    STOPPED(6, "Stopped");

    private final int code;
    private final String description;

    private static final Map<Integer, EddiState> CODE_MAP = new HashMap<>();

    static {
        for (EddiState state : values()) {
            CODE_MAP.put(state.code, state);
        }
    }

    EddiState(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static EddiState fromCode(int code) {
        return CODE_MAP.get(code);
    }
}

