package com.amcglynn.myzappi.api.rest.response;

import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.units.KiloWatt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class EnergyResponse {

    private KiloWatt solarGenerationKW;
    private KiloWatt consumingKW;
    private KiloWatt importingKW;
    private KiloWatt exportingKW;

    public EnergyResponse(ZappiStatusSummary summary) {
        this.solarGenerationKW = new KiloWatt(summary.getGenerated());
        this.consumingKW = new KiloWatt(summary.getConsumed());
        this.importingKW = new KiloWatt(summary.getGridImport());
        this.exportingKW = new KiloWatt(summary.getGridExport());
    }
}
