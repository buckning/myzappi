package com.amcglynn.myzappi.login.rest.response;

import com.amcglynn.myzappi.core.model.SerialNumber;

public class HubDetailsResponse {
    private String hubSerialNumber;
    private String zappiSerialNumber;

    public HubDetailsResponse(String hubSerialNumber, String zappiSerialNumber) {
        this.hubSerialNumber = hubSerialNumber;
        this.zappiSerialNumber = zappiSerialNumber;
    }

    public String getHubSerialNumber() {
        return hubSerialNumber;
    }

    public String getZappiSerialNumber() {
        return zappiSerialNumber;
    }
}
