package com.amcglynn.myenergi.energycost;

public class Tariff {

    public int getStartTime() {
        return this.startTime;
    }

    public int getEndTime() {
        return this.endTime;
    }

    public Type getType() {
        return this.type;
    }

    public double getImportCostPerKwh() {
        return this.importCostPerKwh;
    }

    public double getExportCostPerKwh() {
        return this.exportCostPerKwh;
    }

    public enum Type {
        DAY,
        NIGHT,
        PEAK
    }

    private final int startTime;
    private final int endTime;
    private final Type type;
    private final double importCostPerKwh;
    private final double exportCostPerKwh;


    // build ranges instead of start and end time
    public Tariff(Type type, int startTime, int endTime, double importCostPerKwh, double exportCostPerKwh) {
        this.type = type;
        this.importCostPerKwh = importCostPerKwh;
        this.exportCostPerKwh = exportCostPerKwh;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
