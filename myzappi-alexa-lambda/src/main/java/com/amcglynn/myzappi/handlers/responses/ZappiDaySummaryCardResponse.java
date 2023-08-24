package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.ZappiDaySummary;

import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;

public class ZappiDaySummaryCardResponse {
    private String response;

    public ZappiDaySummaryCardResponse(Locale locale, ZappiDaySummary summary) {
        response = cardResponse(locale, "imported", Map.of("kWh", summary.getImported().toString())) + "\n";
        response += cardResponse(locale, "exported", Map.of("kWh", summary.getExported().toString())) + "\n";
        response += cardResponse(locale, "consumed", Map.of("kWh", summary.getConsumed().toString())) + "\n";
        response += cardResponse(locale, "solar-generated", Map.of("kWh", summary.getSolarGeneration().toString())) + "\n";
        response += cardResponse(locale, "charged", Map.of("kWh", summary.getEvSummary().getTotal().toString())) + "\n";
    }

    @Override
    public String toString() {
        return response;
    }
}
