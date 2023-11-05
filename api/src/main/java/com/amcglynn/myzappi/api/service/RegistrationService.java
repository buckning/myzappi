package com.amcglynn.myzappi.api.service;

import com.amcglynn.myenergi.MyEnergiClientFactory;
import com.amcglynn.myenergi.apiresponse.StatusResponse;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.response.HubDetailsResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class RegistrationService {

    private final LoginService loginService;
    private final MyEnergiClientFactory myEnergiClientFactory;

    public RegistrationService(LoginService loginService, MyEnergiClientFactory myEnergiClientFactory) {
        this.loginService = loginService;
        this.myEnergiClientFactory = myEnergiClientFactory;
    }

    public void register(UserId userId, SerialNumber serialNumber, String apiKey) {
        discoverAndRegisterDetails(userId, serialNumber, apiKey);
    }

    public void delete(UserId userId) {
        log.info("Deleting hub details for user = {}", userId);
        loginService.delete(userId.toString());
    }

    private void discoverAndRegisterDetails(UserId userId, SerialNumber serialNumber, String apiKey) {
        if ("12345678".equals(serialNumber.toString()) && "myDemoApiKey".equals(apiKey)) {
            loginService.register(userId.toString(), serialNumber, serialNumber,
                    new EddiDevice(SerialNumber.from("09876543"), "tank1", "tank2"), apiKey);
            return;
        }

        var myEnergiDevices = discoverMyEnergiDevices(serialNumber, apiKey);
        var zappiSerialNumber = discoverZappi(serialNumber, apiKey, myEnergiDevices);

        if (zappiSerialNumber.isPresent()) {
            var eddi = discoverEddi(myEnergiDevices).orElse(null);
            log.info("Registering zappi = {} hub = {} eddi = {} for user {}", zappiSerialNumber.get(), serialNumber, eddi, userId);
            loginService.register(userId.toString(),
                    zappiSerialNumber.get(), // zappi serial number may be different to gateway/hub
                    serialNumber,
                    eddi,
                    apiKey);
        } else {
            log.warn("Could not find Zappi for system");
            throw new ServerException(409);
        }
    }

    private List<StatusResponse> discoverMyEnergiDevices(SerialNumber serialNumber, String apiKey) {
        var client = myEnergiClientFactory.newMyEnergiClient(serialNumber.toString(), apiKey);
        try {
            return client.getStatus();
        } catch (ClientException e) {
            log.warn("Unexpected error " + e.getMessage(), e);
        }
        return List.of();
    }

    private Optional<SerialNumber> discoverZappi(SerialNumber hubSerialNumber, String apiKey, List<StatusResponse> myEnergiDevices) {
        try {
            var zappis = myEnergiDevices.stream()
                    .filter(statusResponse -> statusResponse.getZappi() != null && !statusResponse.getZappi().isEmpty()).findFirst();
            if (zappis.isPresent() && !zappis.get().getZappi().isEmpty()) {
                var zappiSerialNumber = SerialNumber.from(zappis.get().getZappi().get(0).getSerialNumber());
                var eddi = getEddi(myEnergiDevices).map(EddiDevice::getSerialNumber);
                myEnergiClientFactory.newMyEnergiClient(hubSerialNumber.toString(), zappiSerialNumber.toString(),
                        eddi.map(SerialNumber::toString).orElse(null), apiKey).getZappiStatus();
                return Optional.of(zappiSerialNumber);
            }
            log.warn("Zappi device not found");
        } catch (ClientException e) {
            log.warn("Unexpected error " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    private Optional<EddiDevice> discoverEddi(List<StatusResponse> myEnergiDevices) {
        return getEddi(myEnergiDevices);
    }

    private Optional<EddiDevice> getEddi(List<StatusResponse> statusResponses) {
        var eddis = statusResponses.stream()
                .filter(statusResponse -> statusResponse.getEddi() != null).findFirst();

        if (eddis.isPresent() && !eddis.get().getEddi().isEmpty()) {
            var serialNumber = SerialNumber.from(eddis.get().getEddi().get(0).getSerialNumber());
            var tank1 = eddis.get().getEddi().get(0).getTank1Name();
            var tank2 = eddis.get().getEddi().get(0).getTank2Name();
            return Optional.of(new EddiDevice(serialNumber, tank1, tank2));
        }
        return Optional.empty();
    }

    public Optional<HubDetailsResponse> read(String userId) {
        var details = loginService.readDeploymentDetails(userId);
        return details.map(creds ->
            new HubDetailsResponse(creds.getSerialNumber().toString(), creds.getZappiSerialNumber().toString(),
                    creds.getEddiSerialNumber().map(SerialNumber::toString).orElse(null))
        );
    }

    public void refreshDeploymentDetails(UserId userId) {
        var creds = loginService.readCredentials(userId);
        if (creds.isEmpty()) {
            log.info("User is not registered {}", userId);
            throw new ServerException(404);
        }
        discoverAndRegisterDetails(userId, creds.get().getSerialNumber(), creds.get().getApiKey());
    }
}
