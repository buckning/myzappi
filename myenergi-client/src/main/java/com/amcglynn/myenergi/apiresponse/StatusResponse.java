package com.amcglynn.myenergi.apiresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusResponse {
    private List<MyEnergiDeviceStatus> zappi;
    private List<MyEnergiDeviceStatus> eddi;
    private List<MyEnergiDeviceStatus> libbi;
    private List<MyEnergiDeviceStatus> harvi;
    private int vhub;
    private String fwv;
    private String asn;

    public List<MyEnergiDeviceStatus> getHarvi() {
        return harvi;
    }

    public void setHarvi(List<MyEnergiDeviceStatus> harvi) {
        this.harvi = harvi;
    }

    public Integer getVhub() {
        return vhub;
    }

    public void setVhub(Integer vhub) {
        this.vhub = vhub;
    }

    public String getFwv() {
        return fwv;
    }

    public void setFwv(String fwv) {
        this.fwv = fwv;
    }

    public String getAsn() {
        return asn;
    }

    public void setAsn(String asn) {
        this.asn = asn;
    }

    public List<MyEnergiDeviceStatus> getEddi() {
        return eddi;
    }

    public void setEddi(List<MyEnergiDeviceStatus> eddi) {
        this.eddi = eddi;
    }

    public List<MyEnergiDeviceStatus> getLibbi() {
        return libbi;
    }

    public void setLibbi(List<MyEnergiDeviceStatus> libbi) {
        this.libbi = libbi;
    }

    public List<MyEnergiDeviceStatus> getZappi() {
        return this.zappi;
    }

    public void setZappi(List<MyEnergiDeviceStatus> zappi) {
        this.zappi = zappi;
    }
}
