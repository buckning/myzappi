package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myzappi.core.model.DayCost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class ZappiEnergyCostVoiceResponse {
    private String response;

    private static final Map<String, String> CURRENCY_MAP = Map.of("EUR", "Euro", "GBP", "Pounds");
    private static final Map<String, String> CURRENCY_FRACTION_MAP = Map.of("EUR", "cent", "GBP", "pence");

    public ZappiEnergyCostVoiceResponse(DayCost dayCost) {
        var currencyName = CURRENCY_MAP.get(dayCost.getCurrency());
        var fractionName = CURRENCY_FRACTION_MAP.get(dayCost.getCurrency());

        double cost = dayCost.getImportCost() - dayCost.getExportCost();
        if (cost < 0) {
            response = "Total credit is " + getCostString(Math.abs(cost), currencyName, fractionName) + ". ";
        } else {
            response = "Total cost is " + getCostString(cost, currencyName, fractionName) + ". ";
        }

        response += "You imported " + getCostString(dayCost.getImportCost(), currencyName, fractionName) + ". ";
        response += "You exported " + getCostString(dayCost.getExportCost(), currencyName, fractionName) + ". ";
        response += "Total saved " + getCostString(dayCost.getSolarSavings() + dayCost.getExportCost(), currencyName, fractionName)  + ". ";
    }

    String getCostString(double euro, String currencyName, String fractionName) {
        BigDecimal bigDecimal = new BigDecimal(String.valueOf(euro));
        int intValue = bigDecimal.intValue();

        String result = "";
        if (intValue >= 1) {
            result += intValue + " " + currencyName + " and ";
        }
        BigDecimal cents = bigDecimal.subtract(bigDecimal.setScale(0, RoundingMode.FLOOR))
                .movePointRight(2)
                .abs()
                .remainder(BigDecimal.valueOf(100));
        String centsString = cents.setScale(0, RoundingMode.FLOOR).toPlainString();
        result += centsString + " " + fractionName;
        return result;
    }

    @Override
    public String toString() {
        return response;
    }
}
