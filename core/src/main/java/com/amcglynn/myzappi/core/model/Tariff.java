package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalTime;

@NoArgsConstructor
@ToString
public class Tariff {
    @Getter
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime start;
    @Getter
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime end;
    @Getter
    private String name;
    @Getter
    private double importCostPerKwh;
    @Getter
    private double exportCostPerKwh;

    public Tariff(String name, LocalTime startTime, LocalTime endTime, double importCostPerKwh, double exportCostPerKwh) {
        this.name = name;
        this.importCostPerKwh = importCostPerKwh;
        this.exportCostPerKwh = exportCostPerKwh;
        this.start = startTime;
        this.end = endTime;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }
}
