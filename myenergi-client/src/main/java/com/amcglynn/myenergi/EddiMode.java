package com.amcglynn.myenergi;

public enum EddiMode {
    //0=Stopped, 1=Normal
    STOPPED,
    NORMAL;

    public int getApiValue() {
        return ordinal();
    }
}
