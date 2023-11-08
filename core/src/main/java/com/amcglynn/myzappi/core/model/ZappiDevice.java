package com.amcglynn.myzappi.core.model;

public class ZappiDevice extends MyEnergiDevice {

    public ZappiDevice() {
        this.deviceClass = DeviceClass.ZAPPI;
    }

    public ZappiDevice(SerialNumber serialNumber) {
        super(serialNumber);
        this.deviceClass = DeviceClass.ZAPPI;
    }
}
