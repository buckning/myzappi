package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.MyEnergiClient;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class EddiService {

    private MyEnergiClient client;

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
}
