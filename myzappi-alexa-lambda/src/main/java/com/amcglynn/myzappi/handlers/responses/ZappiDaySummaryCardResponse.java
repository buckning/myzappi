package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.ZappiDaySummary;

public class ZappiDaySummaryCardResponse {
    private String response;

    public ZappiDaySummaryCardResponse(ZappiDaySummary summary) {
        response = "Imported: " + summary.getImported() + "kWh\n";
        response += "Exported: " + summary.getExported() + "kWh\n";
        response += "Consumed: " + summary.getConsumed() + "kWh\n";
        response += "Solar generated: " + summary.getSolarGeneration() + "kWh\n";
        response += "Charged: " + summary.getEvSummary().getTotal() + "kWh\n";
    }

    @Override
    public String toString() {
        return response;
    }
}
