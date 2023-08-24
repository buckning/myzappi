package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.EvStatusSummary;
import com.amcglynn.myenergi.LockStatus;

import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;

public class ZappiEvConnectionStatusCardResponse {
    private String response;

    public ZappiEvConnectionStatusCardResponse(Locale locale, EvStatusSummary summary) {
        if (summary.isConnected()) {
            response = getSummaryForConnected(locale, summary);
            if (summary.getChargeRate().getDouble() >= 0.1) {
                response += cardResponse(locale, "charge-rate", Map.of("kW", summary.getChargeRate().toString()));
            }
        } else {
            response = cardResponse(locale, "ev-not-connected") + "\n";
        }
    }

    private String getSummaryForConnected(Locale locale, EvStatusSummary summary) {
        if (summary.getLockStatus() == LockStatus.LOCKED) {
            return cardResponse(locale, "charger-locked") + "\n";
        }

        if (summary.isFinishedCharging()) {
            return cardResponse(locale, "ev-finished-charging") + "\n"+
                    getChargeMode(locale, summary) +
                    getChargeAddedThisSession(locale, summary);
        }

        return cardResponse(locale, "ev-connected") + "\n" +
                getChargeMode(locale, summary) +
                getChargeAddedThisSession(locale, summary);
    }

    private String getChargeAddedThisSession(Locale locale, EvStatusSummary summary) {
        return cardResponse(locale, "charge-added-this-session", Map.of("kWh", summary.getChargeAddedThisSession().toString())) + "\n";
    }

    private String getChargeMode(Locale locale, EvStatusSummary summary) {
        return cardResponse(locale, "charge-mode", Map.of("chargeMode", summary.getChargeMode().getDisplayName())) + "\n";
    }

    @Override
    public String toString() {
        return response;
    }
}
