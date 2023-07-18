package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myzappi.core.model.DayCost;

import java.util.Currency;

public class ZappiEnergyCostCardResponse {
    private String response;

    public ZappiEnergyCostCardResponse(DayCost dayCost) {
        var currencySymbol = Currency.getInstance(dayCost.getCurrency()).getSymbol();
        response = "Total cost: " + currencySymbol + String.format("%.2f", dayCost.getImportCost() - dayCost.getExportCost()) + "\n";
        response += "Import cost: " + currencySymbol + String.format("%.2f", dayCost.getImportCost()) + "\n";
        response += "Export cost: " + currencySymbol + String.format("%.2f", dayCost.getExportCost()) + "\n";
        response += "Solar consumed saved: " + currencySymbol + String.format("%.2f", dayCost.getSolarSavings()) + "\n";
        response += "Total saved: " + currencySymbol + String.format("%.2f", dayCost.getSolarSavings() + dayCost.getExportCost());
    }

    @Override
    public String toString() {
        return response;
    }
}
