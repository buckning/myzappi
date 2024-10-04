package com.amcglynn.myzappi.graphing;

import com.amcglynn.myzappi.graphing.AxisLabelConverter;
import lombok.SneakyThrows;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.Color;
import java.awt.Dimension;
import java.io.ByteArrayOutputStream;

public class MyEnergiGraph {

    private final XYChart chart;

    private static final int SAMPLES_PER_DAY = 1440;
    // contains an incrementing double array, one for each minute of the day, 1440 in total
    private static final double[] X_DATA = new double[SAMPLES_PER_DAY];

    static {
        for (int i = 0; i < SAMPLES_PER_DAY; i++) {
            X_DATA[i] = i;
        }
    }

    public MyEnergiGraph(Dimension dimension) {
        chart = new XYChartBuilder()
                .width((int) dimension.getWidth())
                .height((int) dimension.getHeight())
                .title("")
                .xAxisTitle("")
                .yAxisTitle("")
                .build();

        applyLegendStyling(chart);
        configurePlotStyling(chart);

        chart.getStyler().setyAxisTickLabelsFormattingFunction(AxisLabelConverter::kiloWattAxisLabelConverter);
        chart.getStyler().setxAxisTickLabelsFormattingFunction(AxisLabelConverter::timeAxisLabelConverter);
    }

    public void addSeries(double[] data, String seriesName, Color color) {
        var series = chart.addSeries(seriesName, X_DATA, data);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineColor(color);
        series.setFillColor(color);
        series.setSmooth(true);
    }

    @SneakyThrows
    public byte[] exportImageBytes() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BitmapEncoder.saveBitmap(chart, baos, BitmapEncoder.BitmapFormat.PNG);
            return baos.toByteArray();
        }
    }

    private void configurePlotStyling(XYChart chart) {
        chart.getStyler().setChartBackgroundColor(Color.BLACK);
        chart.getStyler().setPlotBackgroundColor(Color.black);
        chart.getStyler().setPlotGridLinesVisible(false);
        chart.getStyler().setAxisTickLabelsColor(Color.LIGHT_GRAY);
        chart.getStyler().setPlotBorderVisible(false);
        chart.getStyler().setDefaultSeriesRenderStyle(org.knowm.xchart.XYSeries.XYSeriesRenderStyle.Area);
    }

    private void applyLegendStyling(XYChart chart) {
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setLegendVisible(false);
    }

    public void showLegend(boolean showLegend) {
        chart.getStyler().setLegendVisible(showLegend);
    }
}
