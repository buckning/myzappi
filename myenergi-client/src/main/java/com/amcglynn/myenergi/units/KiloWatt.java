package com.amcglynn.myenergi.units;

import com.fasterxml.jackson.annotation.JsonValue;

public class KiloWatt {
    private final double value;

    public KiloWatt(Watt watt) {
        this.value = (double) watt.getLong() / 1000;
    }

    public double getDouble() {
        return value;
    }

    @JsonValue
    @Override
    public String toString() {
        return String.format("%.1f", value);
    }
}
