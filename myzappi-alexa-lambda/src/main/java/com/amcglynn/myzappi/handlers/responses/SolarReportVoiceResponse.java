package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.units.KiloWatt;
import com.amcglynn.myenergi.units.Watt;

import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class SolarReportVoiceResponse {
    private String response;

    public SolarReportVoiceResponse(Locale locale, ZappiStatusSummary summary) {
        response = getSolarGeneration(locale, summary);
    }

    private String getSolarGeneration(Locale locale, ZappiStatusSummary summary) {
        if (summary.getGenerated().getLong() >= 100L) {
            return voiceResponse(locale, "solar-generation", Map.of("kW", new KiloWatt(summary.getGenerated()).toString()));
        }
        return voiceResponse(locale, "solar-generation", Map.of("kW", new KiloWatt(new Watt(0L)).toString()));
    }

    @Override
    public String toString() {
        return response;
    }
}
