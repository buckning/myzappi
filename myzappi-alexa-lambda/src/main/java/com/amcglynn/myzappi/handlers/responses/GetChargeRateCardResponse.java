package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.units.KiloWatt;

import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;

public class GetChargeRateCardResponse {
    private String response;

    public GetChargeRateCardResponse(Locale locale, ZappiStatusSummary summary) {
        response = "";
        response += getChargingRate(locale, summary);
    }

    private String getChargingRate(Locale locale, ZappiStatusSummary summary) {
        String str = "";
        if (summary.getEvConnectionStatus() == EvConnectionStatus.CHARGING) {
            str += cardResponse(locale, "charge-rate", Map.of("kW", new KiloWatt(summary.getEvChargeRate()).toString()));
        } else if (summary.getChargeStatus() == ChargeStatus.COMPLETE) {
            str += cardResponse(locale, "charging-session-complete");
        } else {
            str += cardResponse(locale, "ev-not-charging");
        }
        return str;
    }


    @Override
    public String toString() {
        return response;
    }
}
