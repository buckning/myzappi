package com.amcglynn.myzappi.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@ToString
public class Tariff {
    @Getter
    private int startTime;
    @Getter
    private int endTime;
    @Getter
    private String name;
    @Getter
    private double importCostPerKwh;
    @Getter
    private double exportCostPerKwh;

    public Tariff(String name, int startTime, int endTime, double importCostPerKwh, double exportCostPerKwh) {
        this.name = name;
        this.importCostPerKwh = importCostPerKwh;
        this.exportCostPerKwh = exportCostPerKwh;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
