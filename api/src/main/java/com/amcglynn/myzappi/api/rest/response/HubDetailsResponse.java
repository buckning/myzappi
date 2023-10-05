package com.amcglynn.myzappi.api.rest.response;

public class HubDetailsResponse {
    private String hubSerialNumber;
    private String zappiSerialNumber;
    private String eddiSerialNumber;

    public HubDetailsResponse(String hubSerialNumber, String zappiSerialNumber, String eddiSerialNumber) {
        this.hubSerialNumber = hubSerialNumber;
        this.zappiSerialNumber = zappiSerialNumber;
        this.eddiSerialNumber = eddiSerialNumber;
    }

    public String getHubSerialNumber() {
        return hubSerialNumber;
    }

    public String getZappiSerialNumber() {
        return zappiSerialNumber;
    }
    public String getEddiSerialNumber() {
        return eddiSerialNumber;
    }
}
