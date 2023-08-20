package com.amcglynn.myzappi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public class Cost {
    private String currencySymbol;
    private int baseCurrencyValue;
    private int subUnitValue;

    public Cost(String currency, double value) {
        BigDecimal bigDecimal = new BigDecimal(String.valueOf(value));
        var cents = bigDecimal.subtract(bigDecimal.setScale(0, RoundingMode.FLOOR))
                .movePointRight(2)
                .abs()
                .remainder(BigDecimal.valueOf(100));

        currencySymbol = Currency.getInstance(currency).getSymbol();

        baseCurrencyValue = bigDecimal.intValue();

        subUnitValue = cents.setScale(0, RoundingMode.FLOOR).intValue();
    }

    @Override
    public String toString() {
        return currencySymbol + baseCurrencyValue + "." + String.format("%02d", subUnitValue);
    }
}
