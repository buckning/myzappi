package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.EvStatusSummary;
import com.amcglynn.myenergi.LockStatus;

import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class ZappiEvConnectionStatusVoiceResponse {
    private String response;

    public ZappiEvConnectionStatusVoiceResponse(Locale locale, EvStatusSummary summary) {
        if (summary.isConnected()) {
            response = getSummaryForConnected(locale, summary);

            if (summary.getChargeRate().getDouble() >= 0.1) {
                response += voiceResponse(locale, "charge-rate", Map.of("kW", summary.getChargeRate().toString()));
            }
        } else {
            response = voiceResponse(locale, "ev-not-connected");
        }
    }

    private String getChargeMode(Locale locale, EvStatusSummary summary) {
        return voiceResponse(locale, "charge-mode", Map.of("chargeMode", summary.getChargeMode().getDisplayName()));
    }

    private String getSummaryForConnected(Locale locale, EvStatusSummary summary) {
        if (summary.isFinishedCharging()) {
            return voiceResponse(locale, "ev-finished-charging") +
                    getChargeMode(locale, summary) +
                    voiceResponse(locale, "charge-added-this-session", Map.of("kWh", summary.getChargeAddedThisSession().toString()));
        }

        if (summary.getLockStatus() == LockStatus.LOCKED) {
            return voiceResponse(locale, "charger-locked");
        }

        return voiceResponse(locale, "ev-connected") +
                getChargeMode(locale, summary) +
                voiceResponse(locale, "charge-added-this-session", Map.of("kWh", summary.getChargeAddedThisSession().toString()));
    }

    @Override
    public String toString() {
        return response;
    }
}
