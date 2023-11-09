package com.amcglynn.myzappi.core.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class EddiDevice extends MyEnergiDevice {

    private String tank1Name;
    private String tank2Name;

    public EddiDevice() {
        this.deviceClass = DeviceClass.EDDI;
    }

    public EddiDevice(SerialNumber serialNumber, String tank1Name, String tank2Name) {
        super(serialNumber);
        this.tank1Name = tank1Name;
        this.tank2Name = tank2Name;
        this.deviceClass = DeviceClass.EDDI;
    }
}
