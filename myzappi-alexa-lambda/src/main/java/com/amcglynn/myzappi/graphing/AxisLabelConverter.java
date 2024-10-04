package com.amcglynn.myzappi.graphing;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AxisLabelConverter {
    /**
     * Convert number of minutes from a day into a time string with format HH:MM
     * @param value number of minutes from a day (0-1439)
     * @return time string with format HH:MM
     */
    public static String timeAxisLabelConverter(Double value) {
        if (value % 120 == 0) {
            return String.format("%02d:%02d", value.intValue() / 60, value.intValue() % 60);
        }
        return "";
    }

    /**
     * Convert a value in kWh to a string with format X.XkW
     * @param value value in kWh
     * @return string with format X.XkW
     */
    public static String kiloWattAxisLabelConverter(Double value) {
        if (value % 1 == 0) {
            return String.format("%.1fkW", value);
        }
        return "";
    }
}
