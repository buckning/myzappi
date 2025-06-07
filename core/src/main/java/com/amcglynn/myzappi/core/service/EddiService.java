package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.EddiState;
import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myenergi.units.Watt;
import com.amcglynn.myzappi.core.model.EddiStatus;
import com.amcglynn.myzappi.core.model.SerialNumber;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class EddiService {

    private final MyEnergiClient client;

    public EddiService(MyEnergiClient client) {
        this.client = client;
    }

    public void setEddiMode(EddiMode mode) {
        log.info("Setting eddi mode to {}", mode);
        client.setEddiMode(mode);
    }

    public void boostEddi(Duration duration) {
        boostEddi(1, duration);
    }

    public void stopEddiBoost() {
        stopEddiBoost(1);
    }

    public void boostEddi(int heaterNumber, Duration duration) {
        validateEddi(heaterNumber);
        client.boostEddi(duration, heaterNumber);
    }

    public void stopEddiBoost(int heaterNumber) {
        validateEddi(heaterNumber);
        client.stopEddiBoost(heaterNumber);
    }

    private void validateEddi(int heaterNumber) {
        if (heaterNumber < 1 || heaterNumber > 2) {
            log.info("Invalid heater number {}", heaterNumber);
            throw new IllegalArgumentException("Invalid heater number");
        }
    }

    public EddiStatus getStatus(SerialNumber serialNumber) {
        var response = client.getEddiStatus(serialNumber.toString()).getEddi().getFirst();
        var gridImport = new Watt(Math.max(0, response.getGridWatts()));
        var gridExport = new Watt(Math.abs(Math.min(0, response.getGridWatts())));
        var generated = new Watt(response.getSolarGeneration());
        return EddiStatus.builder()
                .serialNumber(serialNumber)
                .activeHeater(response.getActiveHeater() == 1 ? response.getTank1Name() : response.getTank2Name())
                .state(EddiState.fromCode(response.getStatus()))
                .consumed(new Watt(generated).add(gridImport).subtract(gridExport))
                .consumedThisSessionKWh(new KiloWattHour((double) response.getEnergyTransferred() / 1000))
                .generated(generated)
                .gridExport(gridExport)
                .gridImport(gridImport)
                .build();
    }
}
