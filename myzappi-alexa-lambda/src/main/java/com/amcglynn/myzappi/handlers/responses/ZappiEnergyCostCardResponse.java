package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myzappi.Cost;
import com.amcglynn.myzappi.core.model.DayCost;

import java.util.Currency;
import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;

public class ZappiEnergyCostCardResponse {
    private String response;

    public ZappiEnergyCostCardResponse(Locale locale, DayCost dayCost) {
        var currencySymbol = Currency.getInstance(dayCost.getCurrency()).getSymbol();
        double cost = dayCost.getImportCost() - dayCost.getExportCost();
        var totalCost = new Cost(dayCost.getCurrency(), Math.abs(cost));

        if (cost < 0) {
            response = getCardResponse(locale, "total-credit", currencySymbol, totalCost) + "\n";
        } else {
            response = getCardResponse(locale, "total-cost", currencySymbol, totalCost) + "\n";
        }

        response += getCardResponse(locale, "import-cost", currencySymbol, new Cost(dayCost.getCurrency(), dayCost.getImportCost())) + "\n";
        response += getCardResponse(locale, "export-cost", currencySymbol, new Cost(dayCost.getCurrency(), dayCost.getExportCost())) + "\n";
        response += getCardResponse(locale, "solar-consumed-saved", currencySymbol, new Cost(dayCost.getCurrency(), dayCost.getSolarSavings())) + "\n";
        response += getCardResponse(locale, "total-saved", currencySymbol, new Cost(dayCost.getCurrency(), dayCost.getSolarSavings() + dayCost.getExportCost()));
    }

    private String getCardResponse(Locale locale, String key, String currencySymbol, Cost cost) {
        return cardResponse(locale, key, Map.of("baseCurrency", currencySymbol,
                "baseCurrencyValue", String.valueOf(cost.getBaseCurrencyValue()),
                "subUnitValue", String.format("%02d", cost.getSubUnitValue())));
    }

    @Override
    public String toString() {
        return response;
    }
}
