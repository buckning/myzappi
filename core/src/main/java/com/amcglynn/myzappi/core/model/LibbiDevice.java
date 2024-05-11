package com.amcglynn.myzappi.core.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = false)
public class LibbiDevice extends MyEnergiDevice {

    public LibbiDevice() {
        this.deviceClass = DeviceClass.LIBBI;
    }

    public LibbiDevice(SerialNumber serialNumber) {
        super(serialNumber);
        this.deviceClass = DeviceClass.LIBBI;
    }
}
