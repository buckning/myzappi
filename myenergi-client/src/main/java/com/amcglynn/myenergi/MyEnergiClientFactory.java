package com.amcglynn.myenergi;

public class MyEnergiClientFactory {

    public MyEnergiClient newMyEnergiClient(String serialNumber, String apiKey) {
        if ("12345678".equals(serialNumber) && "myDemoApiKey".equals(apiKey)) {
            return new MockMyEnergiClient();
        }
        return new MyEnergiClient(serialNumber, apiKey);
    }

    public MyEnergiOAuthClient newMyEnergiOAuthClient(String email, String password) {
        if ("mydemoemail@test.com".equals(email)) {
            return new MockMyEnergiOAuthClient();
        }
        return new MyEnergiOAuthClient(email, password);
    }
}
