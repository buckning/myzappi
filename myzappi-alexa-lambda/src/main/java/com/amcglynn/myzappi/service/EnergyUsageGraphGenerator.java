package com.amcglynn.myzappi.service;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.units.KiloWattHour;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class EnergyUsageGraphGenerator {


    public static void main(String[] args) {
        var client = new MyEnergiClient("insertSerialNumberHere", "insertApiKeyHere");
        var readings = client.getZappiHistory(LocalDate.now().minus(1, ChronoUnit.DAYS)).getReadings();

        double[] xData = new double[readings.size()];
        double[] imported = new double[readings.size()];
        double[] exported = new double[readings.size()];
        double[] consumed = new double[readings.size()];

        for (int i = 0; i < readings.size(); i++) {
            xData[i] = i;

            imported[i] = new KiloWattHour(readings.get(i).getImported()).getDouble();
            exported[i] = new KiloWattHour(readings.get(i).getGridExport()).getDouble() * -1;

            var consumedJoules = readings.get(i).getSolarGeneration()
                    .subtract(readings.get(i).getGridExport());
            consumed[i] = new KiloWattHour(consumedJoules).getDouble();
        }
        EnergyUsageGraphGenerator generator = new EnergyUsageGraphGenerator();
        generator.generateXChart(xData, imported, exported, consumed);
    }

    public void generateXChart(double[] xData, double[] imported, double[] exported, double[] consumed) {

        // Create a chart
        XYChart chart = new XYChartBuilder().width(1200).height(600).title("Multiple Area Chart Example").xAxisTitle("X").yAxisTitle("Y").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setChartBackgroundColor(Color.BLACK);
        chart.getStyler().setPlotBackgroundColor(Color.black);
        chart.getStyler().setPlotGridLinesVisible(false);
        chart.getStyler().setDefaultSeriesRenderStyle(org.knowm.xchart.XYSeries.XYSeriesRenderStyle.Area);




        // Add series
        var series = chart.addSeries("Imported", xData, imported);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineColor(Color.RED);
        series.setFillColor(Color.RED);
        series.setSmooth(true);

        series = chart.addSeries("Exported", xData, exported);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineColor(Color.YELLOW);
        series.setFillColor(Color.YELLOW);
        series.setSmooth(true);

        series = chart.addSeries("Consumed", xData, consumed);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineColor(Color.GREEN);
        series.setFillColor(Color.GREEN);

//        chart.getStyler().setXAxisTickLabelsColor(Color.RED);
//        chart.getStyler().setDatePattern("HH:mm"); // Set the date pattern for the x-axis
//        chart.getStyler().setXAxisDecimalPattern("0"); // Ensure the x-axis uses integer values


        // Save the chart to a ByteArrayOutputStream
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BitmapEncoder.saveBitmap(chart, baos, BitmapEncoder.BitmapFormat.PNG);
            byte[] imageBytes = baos.toByteArray();
            System.out.println("Chart saved to memory. Byte array length: " + imageBytes.length);

            // Write the byte array to a PNG file
            try (FileOutputStream fos = new FileOutputStream("MultipleAreaChart.png")) {
                fos.write(imageBytes);
                System.out.println("Chart saved to MultipleAreaChart.png");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
