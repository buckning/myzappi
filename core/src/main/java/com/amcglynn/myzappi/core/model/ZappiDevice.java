package com.amcglynn.myzappi.core.model;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ZappiDevice extends MyEnergiDevice {

    public ZappiDevice(SerialNumber serialNumber) {
        super(serialNumber);
        this.deviceClass = DeviceClass.ZAPPI;
    }
}
