package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.units.KiloWatt;

import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class GetChargeRateVoiceResponse {
    private String response;

    public GetChargeRateVoiceResponse(Locale locale, ZappiStatusSummary summary) {
        response = getChargingRate(locale, summary);
    }

    private String getChargingRate(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getEvConnectionStatus() == EvConnectionStatus.CHARGING) {
            var chargeType = summary.getChargeStatus() == ChargeStatus.BOOSTING ? "boost-rate-to-ev" : "charge-rate";
            str += voiceResponse(locale, chargeType, Map.of("kW", new KiloWatt(summary.getEvChargeRate()).toString()));
        } else if (summary.getChargeStatus() == ChargeStatus.COMPLETE) {
            str += voiceResponse(locale, "charging-session-complete");
        } else {
            str += voiceResponse(locale, "ev-not-charging");
        }
        return str;
    }

    @Override
    public String toString() {
        return response;
    }
}
