package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonSubTypes({
        @JsonSubTypes.Type(value = ZappiDevice.class, name = "ZAPPI"),
        @JsonSubTypes.Type(value = EddiDevice.class, name = "EDDI"),
        @JsonSubTypes.Type(value = LibbiDevice.class, name = "LIBBI")
})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "deviceClass")
@NoArgsConstructor
@Getter
public abstract class MyEnergiDevice {

    @JsonDeserialize(using = SerialNumberDeserializer.class)
    @JsonSerialize(using = SerialNumberSerializer.class)
    private SerialNumber serialNumber;
    protected DeviceClass deviceClass;

    protected MyEnergiDevice(SerialNumber serialNumber) {
        this.serialNumber = serialNumber;
    }
}
