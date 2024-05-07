package com.amcglynn.myzappi.api.rest.response;

import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.LockStatus;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.units.KiloWatt;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.SerialNumberDeserializer;
import com.amcglynn.myzappi.core.model.SerialNumberSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MyEnergiDeviceStatusResponse {
    /**
     * {
     *     "serialNumber": "12345678",
     *     "type": "zappi",
     *     "mode": "eco",
     *     "firmware": "1.2.3",
     *
     *     "energy": {
     *         "solarGeneration": "20",
     *         "consumpting": "10",
     *         "importing": "5",
     *         "exporting": "2"
     *     },
     *     // zappi fields
     *     "chargeAddedKwh": "10",
     *     "lockStatus": "locked",
     *     "connectionStatus": "connected",
     *     "chargingStatus": "charging",
     *     "chargeRate": "5",
     *
     *     // eddi fields
     *
     *
     *
     *
     *
     *
     *
     *     "boost": {
     *         "status": "enabled",
     *         "kWh": "5",
     *         "duration": "10"
     *     }
     * }
     */
    @JsonDeserialize(using = SerialNumberDeserializer.class)
    @JsonSerialize(using = SerialNumberSerializer.class)
    private SerialNumber serialNumber;
    private DeviceType type;
    private String firmware;
    private EnergyResponse energy;
    private String mode;
    private KiloWattHour chargeAddedKwh;
    private EvConnectionStatus connectionStatus;
    private ChargeStatus chargeStatus;
    private KiloWatt chargeRateKw;

    public MyEnergiDeviceStatusResponse(ZappiStatusSummary summary) {
        this.serialNumber = SerialNumber.from(summary.getSerialNumber());
        this.firmware = summary.getFirmwareVersion();
        this.type = DeviceType.ZAPPI;
        this.energy = new EnergyResponse(summary);
        this.mode = summary.getChargeMode().getDisplayName();
        this.chargeAddedKwh = summary.getChargeAddedThisSession();
        this.connectionStatus = summary.getEvConnectionStatus();
        this.chargeRateKw = new KiloWatt(summary.getEvChargeRate());
        this.chargeStatus = summary.getChargeStatus();
    }
}
