package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.units.KiloWatt;

import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class ZappiStatusSummaryVoiceResponse {
    private String response;

    public ZappiStatusSummaryVoiceResponse(Locale locale, ZappiStatusSummary summary) {
        response = "";
        response += getSolarGeneration(locale, summary);
        response += getGridExport(locale, summary);
        response += getGridImport(locale, summary);
        response += getChargingRate(locale, summary);
        response += getChargeMode(locale, summary);
        response += getChargeComplete(locale, summary);
        response += getChargeAdded(locale, summary);
    }

    private String getSolarGeneration(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getGenerated().getLong() >= 100L) {
            str += voiceResponse(locale, "solar-generation", Map.of("kW", new KiloWatt(summary.getGenerated()).toString()));
        }
        return str;
    }

    private String getGridExport(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getGridExport().getLong() > 0L) {
            str += voiceResponse(locale, "export-rate", Map.of("kW", new KiloWatt(summary.getGridExport()).toString()));
        }
        return str;
    }

    private String getGridImport(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getGridImport().getLong() > 0L) {
            str += voiceResponse(locale, "import-rate", Map.of("kW", new KiloWatt(summary.getGridImport()).toString()));
        }
        return str;
    }

    private String getChargingRate(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getEvConnectionStatus() == EvConnectionStatus.CHARGING) {
            var chargeType = summary.getChargeStatus() == ChargeStatus.BOOSTING ? "boost-rate-to-ev" : "charge-rate";
            str += voiceResponse(locale, chargeType, Map.of("kW", new KiloWatt(summary.getEvChargeRate()).toString()));
        }
        return str;
    }

    private String getChargeMode(Locale locale, ZappiStatusSummary summary) {
        return voiceResponse(locale, "charge-mode", Map.of("chargeMode", summary.getChargeMode().getDisplayName()));
    }

    private String getChargeAdded(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getChargeAddedThisSession().getDouble() > 0.0) {
            str += voiceResponse(locale, "charge-added-this-session", Map.of("kWh", summary.getChargeAddedThisSession().toString()));
        }
        return str;
    }

    private String getChargeComplete(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getChargeStatus() == ChargeStatus.COMPLETE) {
            str += voiceResponse(locale, "charging-session-complete");
        }
        return str;
    }

    @Override
    public String toString() {
        return response;
    }
}
