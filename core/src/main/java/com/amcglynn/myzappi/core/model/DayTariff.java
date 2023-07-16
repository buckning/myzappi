package com.amcglynn.myzappi.core.model;

import java.util.List;

public class DayTariff {
    private String currency;
    private List<Tariff> tariffs;

    public DayTariff(String currency, List<Tariff> tariffs) {
        this.currency = currency;
        this.tariffs = tariffs;
    }

    public String getCurrency() {
        return currency;
    }

    public List<Tariff> getTariffs() {
        return tariffs;
    }
}
