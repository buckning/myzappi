package com.amcglynn.myenergi;

import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myenergi.units.Watt;
import lombok.Getter;

/**
 * This class converts the raw values from the API and provides some convenience methods.
 */
public class ZappiStatusSummary {

    @Getter
    private final String serialNumber;
    @Getter
    private final Watt gridImport;
    @Getter
    private final Watt gridExport;
    @Getter
    private final Watt consumed;
    @Getter
    private final Watt generated;
    @Getter
    private final Watt evChargeRate;
    @Getter
    private final KiloWattHour chargeAddedThisSession;
    @Getter
    private final EvConnectionStatus evConnectionStatus;
    @Getter
    private final ZappiChargeMode chargeMode;
    @Getter
    private final ChargeStatus chargeStatus;
    @Getter
    private final LockStatus lockStatus;
    @Getter
    private final String firmwareVersion;
    @Getter
    private final int mgl;


    public ZappiStatusSummary(ZappiStatus zappiStatus) {
        gridImport = new Watt(Math.max(0, zappiStatus.getGridWatts()));
        gridExport = new Watt(Math.abs(Math.min(0, zappiStatus.getGridWatts())));
        generated = new Watt(zappiStatus.getSolarGeneration());
        consumed = new Watt(generated).add(gridImport).subtract(gridExport);
        // consumed  - charge can be broken down to house and car. House = (consumed - charge)

        chargeStatus = ChargeStatus.values()[zappiStatus.getChargeStatus()];
        chargeMode = ZappiChargeMode.values()[zappiStatus.getZappiChargeMode()];

        chargeAddedThisSession = new KiloWattHour(zappiStatus.getChargeAddedThisSessionKwh());
        evChargeRate = new Watt(zappiStatus.getCarDiversionAmountWatts());
        evConnectionStatus = EvConnectionStatus.fromString(zappiStatus.getEvConnectionStatus());
        lockStatus = LockStatus.from(zappiStatus.getLockStatus());
        serialNumber = zappiStatus.getSerialNumber();
        firmwareVersion = zappiStatus.getFirmwareVersion();
        mgl = zappiStatus.getMgl();
    }

    public String toString() {
        return "ZappiStatusSummary(gridImport=" + this.getGridImport() + ", gridExport=" + this.getGridExport() + ", consumed=" + this.getConsumed() + ", generated=" + this.getGenerated() + ", evChargeRate=" + this.getEvChargeRate() + ", chargeAddedThisSession=" + this.getChargeAddedThisSession() + ", evConnectionStatus=" + this.getEvConnectionStatus() + ", chargeMode=" + this.getChargeMode() + ", chargeStatus=" + this.getChargeStatus() + ")";
    }
}
