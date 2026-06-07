package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.EddiState;
import com.amcglynn.myenergi.LibbiMode;
import com.amcglynn.myenergi.LibbiState;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.apiresponse.MyEnergiDeviceStatus;
import com.amcglynn.myenergi.apiresponse.StatusResponse;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myenergi.units.KiloWatt;
import com.amcglynn.myenergi.units.Watt;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.model.AutomationSnapshot;
import com.amcglynn.myzappi.core.model.EnergyStatus;
import com.amcglynn.myzappi.core.model.SerialNumber;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class AutomationSnapshotMapper {

    public AutomationSnapshot from(List<StatusResponse> statusResponses) {
        var energyStatus = statusResponses.stream()
                .map(this::energyStatus)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new MissingDeviceException("No Zappi, Eddi or Libbi device found"));
        var zappiRates = new HashMap<SerialNumber, KiloWatt>();
        var zappiChargeModes = new HashMap<SerialNumber, ZappiChargeMode>();
        var zappiMinimumGreenLevels = new HashMap<SerialNumber, Integer>();
        var eddiModes = new HashMap<SerialNumber, EddiMode>();
        var libbiModes = new HashMap<SerialNumber, LibbiMode>();
        var libbiStateOfCharge = new HashMap<SerialNumber, Integer>();

        for (StatusResponse statusResponse : statusResponses) {
            Optional.ofNullable(statusResponse.getZappi()).orElse(List.of()).forEach(status -> {
                var serialNumber = SerialNumber.from(status.getSerialNumber());
                zappiRates.put(serialNumber, new KiloWatt(new Watt(status.getCarDiversionAmountWatts())));
                zappiChargeMode(status).ifPresent(mode -> zappiChargeModes.put(serialNumber, mode));
                if (status.getMgl() >= 0) {
                    zappiMinimumGreenLevels.put(serialNumber, status.getMgl());
                }
            });
            Optional.ofNullable(statusResponse.getEddi()).orElse(List.of()).forEach(status ->
                    eddiModes.put(SerialNumber.from(status.getSerialNumber()), eddiMode(status)));
            Optional.ofNullable(statusResponse.getLibbi()).orElse(List.of()).forEach(status -> {
                libbiModes.put(SerialNumber.from(status.getSerialNumber()), libbiMode(status));
                if (status.getStateOfCharge() != null) {
                    libbiStateOfCharge.put(SerialNumber.from(status.getSerialNumber()), status.getStateOfCharge());
                }
            });
        }

        return AutomationSnapshot.builder()
                .energyStatus(energyStatus)
                .zappiEvChargeRateKWBySerialNumber(zappiRates)
                .zappiChargeModeBySerialNumber(zappiChargeModes)
                .zappiMinimumGreenLevelBySerialNumber(zappiMinimumGreenLevels)
                .eddiModeBySerialNumber(eddiModes)
                .libbiModeBySerialNumber(libbiModes)
                .libbiStateOfChargePercentBySerialNumber(libbiStateOfCharge)
                .build();
    }

    private Optional<ZappiChargeMode> zappiChargeMode(ZappiStatus status) {
        var mode = status.getZappiChargeMode();
        if (mode < 0 || mode >= ZappiChargeMode.values().length) {
            return Optional.empty();
        }
        return Optional.of(ZappiChargeMode.values()[mode]);
    }

    private EddiMode eddiMode(MyEnergiDeviceStatus status) {
        return EddiState.fromCode(status.getStatus()) == EddiState.STOPPED ? EddiMode.STOPPED : EddiMode.NORMAL;
    }

    private LibbiMode libbiMode(MyEnergiDeviceStatus status) {
        return LibbiState.from(status.getStatus()) == LibbiState.OFF ? LibbiMode.OFF : LibbiMode.ON;
    }

    private Optional<EnergyStatus> energyStatus(StatusResponse statusResponse) {
        if (statusResponse.getZappi() != null && !statusResponse.getZappi().isEmpty()) {
            return Optional.of(new EnergyStatus(statusResponse.getZappi().get(0)));
        }
        if (statusResponse.getEddi() != null && !statusResponse.getEddi().isEmpty()) {
            return Optional.of(new EnergyStatus(statusResponse.getEddi().get(0)));
        }
        if (statusResponse.getLibbi() != null && !statusResponse.getLibbi().isEmpty()) {
            return Optional.of(new EnergyStatus(statusResponse.getLibbi().get(0)));
        }
        return Optional.empty();
    }
}
