package com.amcglynn.myzappi.core.model;

import com.amcglynn.myzappi.core.service.EnergyCostHourSummary;

public class DayCost {
    private double importCost;
    private double exportCost;
    private double solarSavings;
    private String currency;

    public DayCost(String currency) {
        this.currency = currency;
        importCost = 0.0;
        exportCost = 0.0;
        solarSavings = 0.0;
    }

    public void add(EnergyCostHourSummary hourSummary) {
        this.importCost += hourSummary.getImportCost();
        this.exportCost += hourSummary.getExportCost();
        this.solarSavings += hourSummary.getSolarConsumptionSavings();
    }

    public double getImportCost() {
        return importCost;
    }

    public double getExportCost() {
        return exportCost;
    }

    public double getSolarSavings() {
        return solarSavings;
    }

    public String getCurrency() {
        return currency;
    }
}
