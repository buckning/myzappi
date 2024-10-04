package com.amcglynn.myzappi.graphing;

import com.amcglynn.myenergi.ZappiDaySummary;
import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.units.KiloWattHour;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.awt.Dimension;
import java.awt.Color;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
public class EnergyUsageVisualisation {

    private static final int SAMPLES_PER_DAY = 1440;
    private final Dimension dimension;

    public EnergyUsageVisualisation(Dimension dimension) {
        if (dimension.getHeight() > dimension.getWidth() * 2/3) {
            log.info("The height of the graph is greater than 2/3 of the width. Reducing the height to 2/3 of the width.");
            this.dimension = new Dimension((int) dimension.getWidth(), (int) dimension.getHeight() * 2/3);
        }
        else {
            this.dimension = dimension;
        }
    }

    public byte[] generateGraph(List<ZappiHistory> readings, ZoneId userZone) {
        return generateGraph(readings, null, userZone);
    }

    public byte[] generateGraph(List<ZappiHistory> readings, ZappiDaySummary summary, ZoneId userZone) {
        if (readings.size() < 1440) {
            log.warn("There are not enough readings to generate a graph. Expected at least 1440 readings, but got {}", readings.size());
        }

        double[] imported = new double[SAMPLES_PER_DAY];
        double[] exported = new double[SAMPLES_PER_DAY];
        double[] consumed = new double[SAMPLES_PER_DAY];

        var importedSmoother = new DataSmoother(5);
        var exportedSmoother = new DataSmoother(5);
        var consumedSmoother = new DataSmoother(5);

        for (ZappiHistory reading : readings) {
            var index = calculateIndex(reading, userZone);
            // the readings are in kWh but are only a sample of 1 minute. Multiplying this by 60 scales up the graph so the values make sense to the user
            imported[index] = importedSmoother.smooth(new KiloWattHour(reading.getImported()).getDouble() * 60);

            // multiply by -1 so that the graph shows exports below the x-axis, matching the myenergi app
            exported[index] = exportedSmoother.smooth(new KiloWattHour(reading.getGridExport()).getDouble() * -1 * 60);

            var consumedJoules = reading.getSolarGeneration().subtract(reading.getGridExport());
            consumed[index] = consumedSmoother.smooth(new KiloWattHour(consumedJoules).getDouble() * 60);
        }


        return generateEnergyUsageChartBytes(imported, exported, consumed, summary);
    }

    private int calculateIndex(ZappiHistory zappiHistory, ZoneId userZone) {
        var localTime = getLocalTime(zappiHistory, userZone);
        return localTime.getHour() * 60 + localTime.getMinute();
    }

    /**
     * Zappi API returns the history in UTC. This method will convert the history to the user's time zone.
     * @param history history from myenergi API
     * @param userZone zone that the user is in
     * @return the time converted to the user's time zone
     */
    private LocalTime getLocalTime(ZappiHistory history, ZoneId userZone) {
        return LocalTime.ofInstant(LocalDateTime.of(
                        history.getYear(),
                        history.getMonth(),
                        history.getDayOfMonth(),
                        history.getHour(),
                        history.getMinute())
                .toInstant(ZoneOffset.UTC), userZone);
    }

    @SneakyThrows
    private byte[] generateEnergyUsageChartBytes(double[] imported, double[] exported, double[] consumed, ZappiDaySummary summary) {
        var graph = new MyEnergiGraph(dimension);
        if (summary != null) {
            graph.addSeries(imported, summary.getImported() + "kWh", Color.RED);
            graph.addSeries(exported, summary.getExported() + "kWh", Color.YELLOW);
            graph.addSeries(consumed, summary.getConsumed() + "kWh", Color.GREEN);
            graph.showLegend(true);
        } else {
            graph.addSeries(imported, "Imported", Color.RED);
            graph.addSeries(exported, "Exported", Color.YELLOW);
            graph.addSeries(consumed, "Consumed", Color.GREEN);
        }


        return graph.exportImageBytes();
    }
}
