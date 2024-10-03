package com.amcglynn.myzappi.service;

import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.graphing.DataSmoother;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.Dimension;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
public class EnergyUsageGraphGenerator {

    private static final int SAMPLES_PER_DAY = 1440;
    private final Dimension dimension;

    // contains an incrementing double array
    private static final double[] X_DATA = new double[SAMPLES_PER_DAY];

    static {
        for (int i = 0; i < SAMPLES_PER_DAY; i++) {
            X_DATA[i] = i;
        }
    }

    public EnergyUsageGraphGenerator(Dimension dimension) {
        if (dimension.getHeight() > dimension.getWidth() * 2/3) {
            log.info("The height of the graph is greater than 2/3 of the width. Reducing the height to 2/3 of the width.");
            this.dimension = new Dimension((int) dimension.getWidth(), (int) dimension.getHeight() * 2/3);
        }
        else {
            this.dimension = dimension;
        }
    }

    public byte[] generateGraph(List<ZappiHistory> readings, ZoneId userZone) {
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
            imported[index] = importedSmoother.smooth(new KiloWattHour(reading.getImported()).getDouble());
            exported[index] = exportedSmoother.smooth(new KiloWattHour(reading.getGridExport()).getDouble() * -1);

            var consumedJoules = reading.getSolarGeneration()
                    .subtract(reading.getGridExport());
            consumed[index] = consumedSmoother.smooth(new KiloWattHour(consumedJoules).getDouble());
        }


        return generateChartBytes(imported, exported, consumed);
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
    private byte[] generateChartBytes(double[] imported, double[] exported, double[] consumed) {
        XYChart chart = new XYChartBuilder()
                .width((int) dimension.getWidth())
                .height((int) dimension.getHeight())
                .title("Multiple Area Chart Example")
                .xAxisTitle("X")
                .yAxisTitle("Y")
                .build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setChartBackgroundColor(Color.BLACK);
        chart.getStyler().setPlotBackgroundColor(Color.black);
        chart.getStyler().setPlotGridLinesVisible(false);
        chart.getStyler().setAxisTicksMarksVisible(false);
        chart.getStyler().setPlotBorderVisible(false);
        chart.getStyler().setDefaultSeriesRenderStyle(org.knowm.xchart.XYSeries.XYSeriesRenderStyle.Area);

        var series = chart.addSeries("Imported", X_DATA, imported);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineColor(Color.RED);
        series.setFillColor(Color.RED);
        series.setSmooth(true);

        series = chart.addSeries("Exported", X_DATA, exported);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineColor(Color.YELLOW);
        series.setFillColor(Color.YELLOW);
        series.setSmooth(true);

        series = chart.addSeries("Consumed", X_DATA, consumed);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineColor(Color.GREEN);
        series.setFillColor(Color.GREEN);
        series.setSmooth(true);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BitmapEncoder.saveBitmap(chart, baos, BitmapEncoder.BitmapFormat.PNG);
            return baos.toByteArray();
        }
    }
}
