package com.amcglynn.myenergi;

public enum LibbiMode {
    //0=Stopped, 1=Normal
    OFF,
    ON;

    public int getApiValue() {
        return ordinal();
    }
}
