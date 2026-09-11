package com.amcglynn.myzappi.api.rest.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class EnergyUsageResponse {

    private final String date;
    private final String zoneId;
    private final String resolution;
    private final List<EnergyUsageReading> readings;

    @Getter
    @AllArgsConstructor
    public static class EnergyUsageReading {
        private final String timestamp;
        private final double solarGenerationKWh;
        private final double importedKWh;
        private final double exportedKWh;
        private final double consumedKWh;
        private final double importedKW;
        private final double exportedKW;
        private final double consumedKW;
    }
}
