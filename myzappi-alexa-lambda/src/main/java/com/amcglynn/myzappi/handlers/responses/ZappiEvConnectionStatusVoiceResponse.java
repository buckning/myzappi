package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.EvStatusSummary;
import com.amcglynn.myenergi.LockStatus;

public class ZappiEvConnectionStatusVoiceResponse {
    private String response;

    public ZappiEvConnectionStatusVoiceResponse(EvStatusSummary summary) {
        if (summary.isConnected()) {
            response = getSummaryForConnected(summary);

            if (summary.getChargeRate().getDouble() >= 0.1) {
                response += "Charge rate is " + summary.getChargeRate() + " kilowatts.";
            }
        } else {
            response = "Your E.V. is not connected. ";
        }
    }

    private String getSummaryForConnected(EvStatusSummary summary) {
        if (summary.isFinishedCharging()) {
            return "Your E.V. is finished charging. " +
                    summary.getChargeAddedThisSession() + " kilowatt hours added this session. ";
        }

        if (summary.getLockStatus() == LockStatus.LOCKED) {
            return "Your E.V. is connected but your charger is locked. It needs to be unlocked before you can start charging. ";
        }

        return "Your E.V. is connected. ";
    }

    @Override
    public String toString() {
        return response;
    }
}
