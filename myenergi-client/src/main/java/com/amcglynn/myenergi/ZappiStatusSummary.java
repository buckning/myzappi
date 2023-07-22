package com.amcglynn.myenergi;

import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myenergi.units.Watt;

/**
 * This class converts the raw values from the API and provides some convenience methods.
 */
public class ZappiStatusSummary {

    private Watt gridImport;
    private Watt gridExport;
    private Watt consumed;
    private Watt generated;
    private Watt evChargeRate;
    private KiloWattHour chargeAddedThisSession;
    private EvConnectionStatus evConnectionStatus;
    private ZappiChargeMode chargeMode;
    private ChargeStatus chargeStatus;
    private LockStatus lockStatus;

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
    }

    public Watt getGridImport() {
        return this.gridImport;
    }

    public Watt getGridExport() {
        return this.gridExport;
    }

    public Watt getConsumed() {
        return this.consumed;
    }

    public Watt getGenerated() {
        return this.generated;
    }

    public Watt getEvChargeRate() {
        return this.evChargeRate;
    }

    public KiloWattHour getChargeAddedThisSession() {
        return this.chargeAddedThisSession;
    }

    public EvConnectionStatus getEvConnectionStatus() {
        return this.evConnectionStatus;
    }

    public ZappiChargeMode getChargeMode() {
        return this.chargeMode;
    }

    public ChargeStatus getChargeStatus() {
        return this.chargeStatus;
    }

    public LockStatus getLockStatus() {
        return this.lockStatus;
    }

    public String toString() {
        return "ZappiStatusSummary(gridImport=" + this.getGridImport() + ", gridExport=" + this.getGridExport() + ", consumed=" + this.getConsumed() + ", generated=" + this.getGenerated() + ", evChargeRate=" + this.getEvChargeRate() + ", chargeAddedThisSession=" + this.getChargeAddedThisSession() + ", evConnectionStatus=" + this.getEvConnectionStatus() + ", chargeMode=" + this.getChargeMode() + ", chargeStatus=" + this.getChargeStatus() + ")";
    }
}
