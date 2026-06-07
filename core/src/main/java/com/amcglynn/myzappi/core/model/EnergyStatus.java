package com.amcglynn.myzappi.core.model;

import com.amcglynn.myenergi.apiresponse.MyEnergiDeviceStatus;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
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
        this(status.getSolarGeneration(), status.getGridWatts());
    }

    public EnergyStatus(ZappiStatus status) {
        this(status.getSolarGeneration(), status.getGridWatts());
    }

    private EnergyStatus(Long solarGeneration, Long gridWatts) {
        var gridImport = new Watt(Math.max(0, gridWatts));
        var gridExport = new Watt(Math.abs(Math.min(0, gridWatts)));
        var generated = new Watt(solarGeneration);
        var consumed = new Watt(generated).add(gridImport).subtract(gridExport);

        this.solarGenerationKW = new KiloWatt(generated);
        this.importingKW = new KiloWatt(gridImport);
        this.exportingKW = new KiloWatt(gridExport);
        this.consumingKW = new KiloWatt(consumed);
    }
}
