package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myzappi.core.model.DayCost;
import com.amcglynn.myzappi.Cost;

import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class ZappiEnergyCostVoiceResponse {
    private String response;

    public ZappiEnergyCostVoiceResponse(Locale locale, DayCost dayCost) {
        double cost = dayCost.getImportCost() - dayCost.getExportCost();
        var totalCost = new Cost(dayCost.getCurrency(), Math.abs(cost));

        if (cost < 0) {
            response = getVoiceResponse(locale, "total-credit", totalCost);
        } else {
            response = getVoiceResponse(locale, "total-cost", totalCost);
        }

        var importCost = new Cost(dayCost.getCurrency(), dayCost.getImportCost());
        response += getVoiceResponse(locale, "import-cost", importCost);

        var exportCost = new Cost(dayCost.getCurrency(), dayCost.getExportCost());
        response += getVoiceResponse(locale, "export-cost", exportCost);

        double savings = dayCost.getSolarSavings() + dayCost.getExportCost();
        var solarSavings = new Cost(dayCost.getCurrency(), savings);
        response += getVoiceResponse(locale, "total-saved", solarSavings);
    }

    private String getVoiceResponse(Locale locale, String key, Cost cost) {
        return voiceResponse(locale, key, Map.of("cost", cost.toString()));
    }

    @Override
    public String toString() {
        return response;
    }
}
