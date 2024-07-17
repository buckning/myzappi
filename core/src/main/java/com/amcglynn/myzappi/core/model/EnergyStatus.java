package com.amcglynn.myzappi.core.model;

import com.amcglynn.myenergi.apiresponse.MyEnergiDeviceStatus;
import com.amcglynn.myenergi.units.KiloWatt;
import com.amcglynn.myenergi.units.Watt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class EnergyStatus {

    private KiloWatt solarGenerationKW;
    private KiloWatt consumingKW;
    private KiloWatt importingKW;
    private KiloWatt exportingKW;

    public EnergyStatus(MyEnergiDeviceStatus status) {
        var gridImport = new Watt(Math.max(0, status.getGridWatts()));
        var gridExport = new Watt(Math.abs(Math.min(0, status.getGridWatts())));
        var generated = new Watt(status.getSolarGeneration());
        var consumed = new Watt(generated).add(gridImport).subtract(gridExport);

        this.solarGenerationKW = new KiloWatt(generated);
        this.importingKW = new KiloWatt(gridImport);
        this.exportingKW = new KiloWatt(gridExport);
        this.consumingKW = new KiloWatt(consumed);
    }
}
