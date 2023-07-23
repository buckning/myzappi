package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.EvStatusSummary;
import com.amcglynn.myenergi.LockStatus;

public class ZappiEvConnectionStatusCardResponse {
    private String response;

    public ZappiEvConnectionStatusCardResponse(EvStatusSummary summary) {
        if (summary.isConnected()) {
            response = getSummaryForConnected(summary);
            if (summary.getChargeRate().getDouble() >= 0.1) {
                response += "Charge rate is " + summary.getChargeRate() + "kW.";
            }
        } else {
            response = "Your E.V. is not connected.\n";
        }
    }

    private String getSummaryForConnected(EvStatusSummary summary) {
        if (summary.isFinishedCharging()) {
            return "Your E.V. is finished charging. " +
                    summary.getChargeAddedThisSession() + "kWh added this session.\n";
        }
        if (summary.getLockStatus() == LockStatus.LOCKED) {
            return "Your E.V. is connected but your charger is locked. It needs to be unlocked before you can start charging.\n";
        }

        return "Your E.V. is connected.\n";
    }

    @Override
    public String toString() {
        return response;
    }
}
