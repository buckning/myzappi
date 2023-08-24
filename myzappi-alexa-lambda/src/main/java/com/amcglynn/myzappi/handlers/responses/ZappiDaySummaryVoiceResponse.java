package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.ZappiDaySummary;

import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class ZappiDaySummaryVoiceResponse {
    private String response;

    public ZappiDaySummaryVoiceResponse(Locale locale, ZappiDaySummary summary) {
        response = voiceResponse(locale, "imported", Map.of("kWh", summary.getImported().toString()));
        response += voiceResponse(locale, "exported", Map.of("kWh", summary.getExported().toString()));
        response += voiceResponse(locale, "consumed", Map.of("kWh", summary.getConsumed().toString()));
        response += voiceResponse(locale, "solar-generated", Map.of("kWh", summary.getSolarGeneration().toString()));
        response += voiceResponse(locale, "charged", Map.of("kWh", summary.getEvSummary().getTotal().toString()));
    }

    @Override
    public String toString() {
        return response;
    }
}
