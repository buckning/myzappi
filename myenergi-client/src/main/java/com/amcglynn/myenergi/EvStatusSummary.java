package com.amcglynn.myenergi;

import com.amcglynn.myenergi.units.KiloWatt;
import com.amcglynn.myenergi.units.KiloWattHour;

import java.util.List;

public class EvStatusSummary {

    private static final List<EvConnectionStatus> CONNECTED_STATUSES = List.of(EvConnectionStatus.EV_CONNECTED,
            EvConnectionStatus.READY_TO_CHARGE, EvConnectionStatus.CHARGING, EvConnectionStatus.WAITING_FOR_EV);

    private EvConnectionStatus evConnectionStatus;
    private KiloWatt chargeRate;
    private KiloWattHour chargeAddedThisSession;
    private LockStatus lockStatus;

    private ZappiChargeMode chargeMode;

    public EvStatusSummary(ZappiStatusSummary status) {
        this.chargeAddedThisSession = status.getChargeAddedThisSession();
        this.evConnectionStatus = status.getEvConnectionStatus();
        this.chargeRate = new KiloWatt(status.getEvChargeRate());
        this.lockStatus = status.getLockStatus();
        this.chargeMode = status.getChargeMode();
    }

    public KiloWatt getChargeRate() {
        return chargeRate;
    }

    public KiloWattHour getChargeAddedThisSession() {
        return chargeAddedThisSession;
    }

    public boolean isConnected() {
        return CONNECTED_STATUSES.contains(evConnectionStatus);
    }

    public boolean isFinishedCharging() {
        return evConnectionStatus == EvConnectionStatus.WAITING_FOR_EV;
    }

    public LockStatus getLockStatus() {
        return lockStatus;
    }

    public ZappiChargeMode getChargeMode() {
        return chargeMode;
    }

}
