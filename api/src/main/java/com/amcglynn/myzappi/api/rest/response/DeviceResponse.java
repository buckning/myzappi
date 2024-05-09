package com.amcglynn.myzappi.api.rest.response;

import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.SerialNumberDeserializer;
import com.amcglynn.myzappi.core.model.SerialNumberSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

@Getter
public class DeviceResponse {
    @JsonDeserialize(using = SerialNumberDeserializer.class)
    @JsonSerialize(using = SerialNumberSerializer.class)
    private SerialNumber serialNumber;
    protected DeviceType type;

    public DeviceResponse(MyEnergiDevice device) {
        this.serialNumber = device.getSerialNumber();
        this.type = DeviceType.valueOf(device.getDeviceClass().name());
    }
}
