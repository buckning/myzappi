package com.amcglynn.myzappi.core.model;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.LibbiMode;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.units.KiloWatt;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

@Getter
@Builder
public class AutomationSnapshot {
    private final EnergyStatus energyStatus;
    private final Map<SerialNumber, KiloWatt> zappiEvChargeRateKWBySerialNumber;
    private final Map<SerialNumber, ZappiChargeMode> zappiChargeModeBySerialNumber;
    private final Map<SerialNumber, Integer> zappiMinimumGreenLevelBySerialNumber;
    private final Map<SerialNumber, EddiMode> eddiModeBySerialNumber;
    private final Map<SerialNumber, LibbiMode> libbiModeBySerialNumber;
    private final Map<SerialNumber, Integer> libbiStateOfChargePercentBySerialNumber;

    public KiloWatt getZappiEvChargeRateKW(SerialNumber serialNumber) {
        var rate = zappiEvChargeRateKWBySerialNumber.get(serialNumber);
        if (rate == null) {
            throw new IllegalArgumentException("No Zappi EV charge rate found for " + serialNumber);
        }
        return rate;
    }

    public Optional<Integer> getLibbiStateOfChargePercent(SerialNumber serialNumber) {
        return Optional.ofNullable(libbiStateOfChargePercentBySerialNumber.get(serialNumber));
    }

    public Optional<ZappiChargeMode> getZappiChargeMode(SerialNumber serialNumber) {
        return Optional.ofNullable(zappiChargeModeBySerialNumber.get(serialNumber));
    }

    public Optional<Integer> getZappiMinimumGreenLevel(SerialNumber serialNumber) {
        return Optional.ofNullable(zappiMinimumGreenLevelBySerialNumber.get(serialNumber));
    }

    public Optional<EddiMode> getEddiMode(SerialNumber serialNumber) {
        return Optional.ofNullable(eddiModeBySerialNumber.get(serialNumber));
    }

    public Optional<LibbiMode> getLibbiMode(SerialNumber serialNumber) {
        return Optional.ofNullable(libbiModeBySerialNumber.get(serialNumber));
    }
}
