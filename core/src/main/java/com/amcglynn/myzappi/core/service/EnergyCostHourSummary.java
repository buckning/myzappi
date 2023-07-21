package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.model.Tariff;

public class EnergyCostHourSummary {

    private final KiloWattHour imported;
    private final KiloWattHour exported;
    private final KiloWattHour solarConsumption;
    private final Tariff tariff;

    public EnergyCostHourSummary(Tariff tariff, ZappiHistory history) {
        this.tariff = tariff;
        this.imported = new KiloWattHour(history.getImported());
        this.exported = new KiloWattHour(history.getGridExport());
        this.solarConsumption = new KiloWattHour(history.getSolarGeneration()).minus(exported);
    }

    public double getSolarConsumptionSavings() {
        // consumption is what is generated minus what is exported. Saving is based on what the cost of import since that was what it would have cost if it was imported.
        return tariff.getImportCostPerKwh() * solarConsumption.getDouble();
    }

    public double getImportCost() {
        return tariff.getImportCostPerKwh() * imported.getDouble();
    }

    public double getExportCost() {
        return tariff.getExportCostPerKwh() * exported.getDouble();
    }
}
