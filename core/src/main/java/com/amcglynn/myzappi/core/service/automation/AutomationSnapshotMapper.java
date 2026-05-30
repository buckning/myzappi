package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myenergi.apiresponse.MyEnergiDeviceStatus;
import com.amcglynn.myenergi.apiresponse.StatusResponse;
import com.amcglynn.myenergi.units.KiloWatt;
import com.amcglynn.myenergi.units.Watt;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.model.AutomationSnapshot;
import com.amcglynn.myzappi.core.model.EnergyStatus;
import com.amcglynn.myzappi.core.model.SerialNumber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class AutomationSnapshotMapper {

    public AutomationSnapshot from(List<StatusResponse> statusResponses) {
        var firstDeviceStatus = statusResponses.stream()
                .flatMap(statusResponse -> devices(statusResponse).stream())
                .findFirst()
                .orElseThrow(() -> new MissingDeviceException("No Zappi, Eddi or Libbi device found"));
        var zappiRates = new HashMap<SerialNumber, KiloWatt>();
        var libbiStateOfCharge = new HashMap<SerialNumber, Integer>();

        for (StatusResponse statusResponse : statusResponses) {
            Optional.ofNullable(statusResponse.getZappi()).orElse(List.of()).forEach(status ->
                    zappiRates.put(SerialNumber.from(status.getSerialNumber()),
                            new KiloWatt(new Watt(status.getDiversionAmountWatts()))));
            Optional.ofNullable(statusResponse.getLibbi()).orElse(List.of()).forEach(status -> {
                if (status.getStateOfCharge() != null) {
                    libbiStateOfCharge.put(SerialNumber.from(status.getSerialNumber()), status.getStateOfCharge());
                }
            });
        }

        return AutomationSnapshot.builder()
                .energyStatus(new EnergyStatus(firstDeviceStatus))
                .zappiEvChargeRateKWBySerialNumber(zappiRates)
                .libbiStateOfChargePercentBySerialNumber(libbiStateOfCharge)
                .build();
    }

    private List<MyEnergiDeviceStatus> devices(StatusResponse statusResponse) {
        var devices = new ArrayList<MyEnergiDeviceStatus>();
        devices.addAll(Optional.ofNullable(statusResponse.getZappi()).orElse(List.of()));
        devices.addAll(Optional.ofNullable(statusResponse.getEddi()).orElse(List.of()));
        devices.addAll(Optional.ofNullable(statusResponse.getLibbi()).orElse(List.of()));
        return devices;
    }
}
