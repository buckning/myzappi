package com.amcglynn.myzappi.core.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SerialNumber {

    @NonNull
    private final String serialNumber;

    public static SerialNumber from(String serialNumber) {
        return new SerialNumber(serialNumber);
    }

    public String toString() {
        return serialNumber;
    }
}
