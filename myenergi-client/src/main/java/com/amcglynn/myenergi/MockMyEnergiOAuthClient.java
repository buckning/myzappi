package com.amcglynn.myenergi;

import com.amcglynn.myenergi.apiresponse.LibbiChargeSetupResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockMyEnergiOAuthClient extends MyEnergiOAuthClient {

    public MockMyEnergiOAuthClient() {
        super();
    }

    @Override
    public String getUserHubsAndDevices() {
        return "mockResponse";
    }

    @Override
    public void setChargeFromGrid(String serialNumber, boolean chargeFromGrid) {

    }

    @Override
    public void setTargetEnergy(String serialNumber, int targetEnergy) {

    }

    @Override
    public LibbiChargeSetupResponse getLibbiChargeSetup(String serialNumber) {
        return new LibbiChargeSetupResponse(serialNumber, true, 4600);
    }
}
