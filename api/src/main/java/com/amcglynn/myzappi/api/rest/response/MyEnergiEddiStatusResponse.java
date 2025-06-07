package com.amcglynn.myzappi.api.rest.response;

import com.amcglynn.myenergi.EddiState;
import com.amcglynn.myenergi.units.KiloWatt;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.model.EddiStatus;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.SerialNumberDeserializer;
import com.amcglynn.myzappi.core.model.SerialNumberSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class MyEnergiEddiStatusResponse {

    @JsonDeserialize(using = SerialNumberDeserializer.class)
    @JsonSerialize(using = SerialNumberSerializer.class)
    private SerialNumber serialNumber;
    private DeviceType type;
    private EnergyResponse energy;
    private KiloWatt diversionAmountKW;

    private EddiState state;
    private String activeHeater;
    private KiloWattHour consumedThisSessionKWh;
    private String tank1Name;
    private String tank2Name;

    public MyEnergiEddiStatusResponse(EddiStatus summary) {
        this.type = DeviceType.EDDI;
        this.serialNumber = summary.getSerialNumber();
        this.energy = new EnergyResponse(summary);
        this.state = summary.getState();
        this.activeHeater = summary.getActiveHeater();
        this.consumedThisSessionKWh = summary.getConsumedThisSessionKWh();
        this.diversionAmountKW = new KiloWatt(summary.getDiversionAmountWatts());
        this.tank1Name = summary.getTank1Name();
        this.tank2Name = summary.getTank2Name();
    }
}
