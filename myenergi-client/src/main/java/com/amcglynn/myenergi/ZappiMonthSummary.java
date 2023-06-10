package com.amcglynn.myenergi;

import com.amcglynn.myenergi.units.KiloWattHour;

import java.time.YearMonth;
import java.util.List;

public class ZappiMonthSummary {
    private YearMonth yearMonth;
    private final KiloWattHour solarGeneration;
    private final KiloWattHour exported;
    private final KiloWattHour imported;
    private final KiloWattHour evTotal;

    public ZappiMonthSummary(YearMonth yearMonth, List<ZappiDaySummary> dataPoints) {
        this.yearMonth = yearMonth;
        var solarGenerationTemp = new KiloWattHour(0);
        var exportedTemp = new KiloWattHour(0);
        var importedTemp = new KiloWattHour(0);
        var evTotalTemp = new KiloWattHour(0);

        for (var dp : dataPoints) {
            solarGenerationTemp = solarGenerationTemp.add(dp.getSolarGeneration());
            exportedTemp = exportedTemp.add(dp.getExported());
            importedTemp = importedTemp.add(dp.getImported());
            evTotalTemp = evTotalTemp.add(dp.getEvSummary().getTotal());
        }

        this.solarGeneration = new KiloWattHour(solarGenerationTemp);
        this.exported = new KiloWattHour(exportedTemp);
        this.imported = new KiloWattHour(importedTemp);
        this.evTotal = new KiloWattHour(evTotalTemp);
    }

    public YearMonth getYearMonth() {
        return this.yearMonth;
    }

    public KiloWattHour getSolarGeneration() {
        return this.solarGeneration;
    }

    public KiloWattHour getExported() {
        return this.exported;
    }

    public KiloWattHour getImported() {
        return this.imported;
    }

    public KiloWattHour getEvTotal() {
        return this.evTotal;
    }

    public String toString() {
        return "ZappiMonthSummary(yearMonth=" + this.getYearMonth() + ", solarGeneration=" + this.getSolarGeneration() + ", exported=" + this.getExported() + ", imported=" + this.getImported() + ", evTotal=" + this.getEvTotal() + ")";
    }
}
