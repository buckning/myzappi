package com.amcglynn.myzappi.graphing;

import lombok.AllArgsConstructor;

import java.util.ArrayDeque;
import java.util.Queue;

@AllArgsConstructor
public class DataSmoother {

    private final int windowSize;
    private final Queue<Double> history;
    private double sum;

    public DataSmoother(int windowSize) {
        this.windowSize = windowSize;
        this.history = new ArrayDeque<>();
        this.sum = 0.0;
    }

    public double smooth(double currentValue) {
        history.add(currentValue);
        sum += currentValue;

        if (history.size() > windowSize) {
            double oldestValue = history.poll();
            sum -= oldestValue;
        }

        return sum / history.size();
    }
}
