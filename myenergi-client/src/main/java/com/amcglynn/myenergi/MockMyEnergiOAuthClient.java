package com.amcglynn.myenergi;

import com.amcglynn.myenergi.apiresponse.LibbiChargeSetupResponse;
import com.amcglynn.myenergi.apiresponse.MyEnergiResponse;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myenergi.exception.ServerCommunicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;

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
