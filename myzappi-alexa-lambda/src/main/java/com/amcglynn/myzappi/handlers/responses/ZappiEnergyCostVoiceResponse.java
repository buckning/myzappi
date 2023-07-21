package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myzappi.core.model.DayCost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class ZappiEnergyCostVoiceResponse {
    private String response;

    private static final Map<String, String> CURRENCY_MAP = Map.of("EUR", "Euro", "GBP", "Pound");
    private static final Map<String, String> CURRENCY_PLURAL_MAP = Map.of("EUR", "Euro", "GBP", "Pounds");
    private static final Map<String, String> CURRENCY_FRACTION_MAP = Map.of("EUR", "cent", "GBP", "pence");

    public ZappiEnergyCostVoiceResponse(DayCost dayCost) {
        var fractionName = CURRENCY_FRACTION_MAP.get(dayCost.getCurrency());

        double cost = dayCost.getImportCost() - dayCost.getExportCost();
        if (cost < 0) {
            response = "Total credit is " + getCostString(Math.abs(cost), getCurrency(cost, dayCost.getCurrency()), fractionName) + ". ";
        } else {
            response = "Total cost is " + getCostString(cost, getCurrency(cost, dayCost.getCurrency()), fractionName) + ". ";
        }

        response += "You imported " + getCostString(dayCost.getImportCost(), getCurrency(dayCost.getImportCost(), dayCost.getCurrency()), fractionName) + ". ";
        response += "You exported " + getCostString(dayCost.getExportCost(), getCurrency(dayCost.getExportCost(), dayCost.getCurrency()), fractionName) + ". ";
        double savings = dayCost.getSolarSavings() + dayCost.getExportCost();
        response += "Total saved " + getCostString(savings, getCurrency(savings, dayCost.getCurrency()), fractionName)  + ". ";
    }

    private String getCurrency(double cost, String currency) {
        double abs = Math.abs(cost);
        if (abs >= 1.0 && abs < 2.0) {
            return CURRENCY_MAP.get(currency);
        }
        return CURRENCY_PLURAL_MAP.get(currency);
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
