package com.amcglynn.myzappi.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class Cost {

    private static final Map<String, String> BASE_CURRENCY_MAP = Map.of("EUR", "Euro", "GBP", "Pound");
    private static final Map<String, String> BASE_CURRENCY_PLURAL_MAP = Map.of("EUR", "Euro", "GBP", "Pounds");
    private static final Map<String, String> CURRENCY_SUBUNIT_MAP = Map.of("EUR", "cent", "GBP", "pence");

    private String baseCurrency;
    private String subUnit;
    private int baseCurrencyValue;
    private int subUnitValue;

    public Cost(String currency, double value) {
        BigDecimal bigDecimal = new BigDecimal(String.valueOf(value));
        var cents = new BigDecimal(String.valueOf(Math.abs(value))).subtract(bigDecimal.setScale(0, RoundingMode.FLOOR))
                .movePointRight(2)
                .abs()
                .remainder(BigDecimal.valueOf(100));

        baseCurrencyValue = bigDecimal.intValue();

        subUnitValue = cents.setScale(0, RoundingMode.FLOOR).intValue();

        subUnit = CURRENCY_SUBUNIT_MAP.get(currency);

        baseCurrency = getBaseCurrency(value, currency);
    }

    private String getBaseCurrency(double cost, String currency) {
        double abs = Math.abs(cost);
        if (abs >= 1.0 && abs < 2.0) {
            return BASE_CURRENCY_MAP.get(currency);
        }
        return BASE_CURRENCY_PLURAL_MAP.get(currency);
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getSubUnit() {
        return subUnit;
    }

    public int getBaseCurrencyValue() {
        return baseCurrencyValue;
    }

    public int getSubUnitValue() {
        return subUnitValue;
    }

    public double to2DecimalPlaces() {
        var doubleStr = String.valueOf(getBaseCurrencyValue());
        doubleStr += "." + String.format("%02d", getSubUnitValue());

        return Double.parseDouble(doubleStr);
    }
}
