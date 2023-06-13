package com.amcglynn.myenergi;

public enum ZappiBoostMode {
    // 2=Stop, 10=Boost, 11=SmartBoost
    STOP(2),
    BOOST(10),
    SMART_BOOST(11),
    OFF(0);

    private final int boostValue;

    public int getBoostValue() {
        return this.boostValue;
    }

    ZappiBoostMode(int boostValue) {
        this.boostValue = boostValue;
    }
}
