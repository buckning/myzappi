package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.units.KiloWatt;

import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;

public class ZappiStatusSummaryCardResponse {
    private String response;

    public ZappiStatusSummaryCardResponse(Locale locale, ZappiStatusSummary summary) {
        response = "";
        response += getSolarGeneration(locale, summary);
        response += getGridExport(locale, summary);
        response += getGridImport(locale, summary);
        response += getChargingRate(locale, summary);
        response += getChargeMode(locale, summary);
        response += getBoostMode(locale, summary);
        response += getChargeComplete(locale, summary);
        response += getChargeAdded(locale, summary);
    }

    private String getSolarGeneration(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getGenerated().getLong() >= 100L) {
            str += cardResponse(locale, "solar-generation", Map.of("kW", new KiloWatt(summary.getGenerated()).toString())) + "\n";
        }
        return str;
    }

    private String getGridExport(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getGridExport().getLong() > 0L) {
            str += cardResponse(locale, "export-rate", Map.of("kW", new KiloWatt(summary.getGridExport()).toString())) + "\n";
        }
        return str;
    }

    private String getGridImport(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getGridImport().getLong() > 0L) {
            str += cardResponse(locale, "import-rate", Map.of("kW", new KiloWatt(summary.getGridImport()).toString())) + "\n";
        }
        return str;
    }

    private String getChargingRate(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getEvConnectionStatus() == EvConnectionStatus.CHARGING) {
            str += cardResponse(locale, "charge-rate", Map.of("kW", new KiloWatt(summary.getEvChargeRate()).toString())) + "\n";
        }
        return str;
    }

    private String getBoostMode(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getChargeStatus() == ChargeStatus.BOOSTING) {
            str += cardResponse(locale, "boost-enabled") + "\n";
        }
        return str;
    }

    private String getChargeMode(Locale locale, ZappiStatusSummary summary) {
        return cardResponse(locale, "charge-mode", Map.of("chargeMode",summary.getChargeMode().getDisplayName())) + "\n";
    }

    private String getChargeAdded(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getChargeAddedThisSession().getDouble() > 0.01) {
            str += cardResponse(locale, "charge-added-this-session", Map.of("kWh", summary.getChargeAddedThisSession().toString())) + "\n";
        }
        return str;
    }

    private String getChargeComplete(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getChargeStatus() == ChargeStatus.COMPLETE) {
            str += cardResponse(locale, "charging-session-complete") + "\n";
        }
        return str;
    }

    @Override
    public String toString() {
        return response;
    }
}
