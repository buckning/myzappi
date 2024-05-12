package com.amcglynn.myzappi.api.rest.response;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DeviceType {
    ZAPPI("zappi"),
    EDDI("eddi"),
    LIBBI("libbi");

    private final String type;

    DeviceType(String type) {
        this.type = type;
    }

    @JsonValue
    @Override
    public String toString() {
        return type;
    }
}
