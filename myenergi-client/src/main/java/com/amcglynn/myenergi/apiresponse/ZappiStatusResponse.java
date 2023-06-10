package com.amcglynn.myenergi.apiresponse;

import java.util.List;

public class ZappiStatusResponse {
    List<ZappiStatus> zappi;

    public List<ZappiStatus> getZappi() {
        return this.zappi;
    }

    public void setZappi(List<ZappiStatus> zappi) {
        this.zappi = zappi;
    }
}
