package com.amcglynn.myzappi.api.rest.response;

import lombok.Getter;

import java.util.List;

@Getter
public class ListDeviceResponse {
    private final List<DeviceResponse> devices;

    public ListDeviceResponse(List<DeviceResponse> devices) {
        this.devices = devices;
    }
}
