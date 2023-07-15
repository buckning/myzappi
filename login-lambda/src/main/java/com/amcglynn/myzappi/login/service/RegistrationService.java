package com.amcglynn.myzappi.login.service;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.login.UserId;
import com.amcglynn.myzappi.login.rest.ServerException;

import java.util.Optional;

public class RegistrationService {

    private final LoginService loginService;

    public RegistrationService(LoginService loginService) {
        this.loginService = loginService;
    }

    public void register(UserId userId, SerialNumber serialNumber, String apiKey) {
        discoverAndRegisterDetails(userId, serialNumber, apiKey);
    }

    public void delete(UserId userId) {
        System.out.println("Deleting hub details for user = " + userId);
        loginService.delete(userId.toString());
    }

    private void discoverAndRegisterDetails(UserId userId, SerialNumber serialNumber, String apiKey) {
        if ("12345678".equals(serialNumber.toString()) && "myDemoApiKey".equals(apiKey)) {
            loginService.register(userId.toString(), serialNumber, serialNumber, apiKey);
            return;
        }

        var zappiSerialNumber = discover(serialNumber, apiKey);

        if (zappiSerialNumber.isPresent()) {
            System.out.println("Registering zappi = " + zappiSerialNumber.get() + " hub = " + serialNumber + " for user " + userId);
            loginService.register(userId.toString(),
                    zappiSerialNumber.get(), // zappi serial number may be different to gateway/hub
                    serialNumber, apiKey);
        } else {
            System.err.println("Could not find Zappi for system");
            throw new ServerException(409);
        }
    }

    private Optional<SerialNumber> discover(SerialNumber serialNumber, String apiKey) {
        var client = new MyEnergiClient(serialNumber.toString(), apiKey);
        try {
            var zappis = client.getStatus().stream()
                    .filter(statusResponse -> statusResponse.getZappi() != null).findFirst();
            if (zappis.isPresent() && !zappis.get().getZappi().isEmpty()) {
                return Optional.of(SerialNumber.from(zappis.get().getZappi().get(0).getSerialNumber()));
            }
            System.out.println("Zappi device not found");
        } catch (ClientException e) {
            System.out.println("Unexpected error " + e.getMessage());
        }
        return Optional.empty();
    }
}
