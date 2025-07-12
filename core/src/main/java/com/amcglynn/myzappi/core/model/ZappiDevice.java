package com.amcglynn.myzappi.core.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = false)
public class ZappiDevice extends MyEnergiDevice {

    public ZappiDevice() {
        this.deviceClass = DeviceClass.ZAPPI;
    }

    public ZappiDevice(SerialNumber serialNumber) {
        super(serialNumber);
        this.deviceClass = DeviceClass.ZAPPI;
    }
}
