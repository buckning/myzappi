package com.amcglynn.myzappi.core.model;

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
}
