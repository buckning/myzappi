package com.amcglynn.myenergi;

public class MyEnergiClientFactory {

    public MyEnergiClient newMyEnergiClient(String serialNumber, String apiKey) {
        return new MyEnergiClient(serialNumber, apiKey);
    }

    public MyEnergiClient newMyEnergiClient(String hubSerialNumber, String zappiSerialNumber, String eddiSerialNumber, String apiKey) {
        return new MyEnergiClient(zappiSerialNumber, hubSerialNumber, eddiSerialNumber, apiKey);
    }
}
