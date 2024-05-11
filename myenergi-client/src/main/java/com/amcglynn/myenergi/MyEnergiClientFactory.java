package com.amcglynn.myenergi;

public class MyEnergiClientFactory {

    public MyEnergiClient newMyEnergiClient(String serialNumber, String apiKey) {
        if ("12345678".equals(serialNumber) && "myDemoApiKey".equals(apiKey)) {
            return new MockMyEnergiClient();
        }
        return new MyEnergiClient(serialNumber, apiKey);
    }

    public MyEnergiClient newMyEnergiClient(String hubSerialNumber, String zappiSerialNumber, String eddiSerialNumber, String apiKey) {
        if ("12345678".equals(hubSerialNumber) && "myDemoApiKey".equals(apiKey)) {
            return new MockMyEnergiClient();
        }
        return new MyEnergiClient(zappiSerialNumber, hubSerialNumber, eddiSerialNumber, apiKey);
    }
}
