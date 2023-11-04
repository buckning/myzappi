package com.amcglynn.myzappi.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class EddiDevice extends MyEnergiDevice {

    private String tank1Name;
    private String tank2Name;

    public EddiDevice(SerialNumber serialNumber, String tank1Name, String tank2Name) {
        super(serialNumber);
        this.tank1Name = tank1Name;
        this.tank2Name = tank2Name;
        this.deviceClass = DeviceClass.EDDI;
    }
}
