package com.amcglynn.myzappi.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class HubCredentials {
    private final SerialNumber serialNumber;
    private final String apiKey;
}
