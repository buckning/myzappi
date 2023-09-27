package com.amcglynn.myzappi.api.service;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.response.HubDetailsResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class RegistrationService {

    private final LoginService loginService;

    public RegistrationService(LoginService loginService) {
        this.loginService = loginService;
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
            loginService.register(userId.toString(), serialNumber, serialNumber, apiKey);
            return;
        }

        var zappiSerialNumber = discover(serialNumber, apiKey);

        if (zappiSerialNumber.isPresent()) {
            log.info("Registering zappi = {} hub = {} for user {}", zappiSerialNumber.get(), serialNumber, userId);
            loginService.register(userId.toString(),
                    zappiSerialNumber.get(), // zappi serial number may be different to gateway/hub
                    serialNumber, apiKey);
        } else {
            log.warn("Could not find Zappi for system");
            throw new ServerException(409);
        }
    }

    private Optional<SerialNumber> discover(SerialNumber serialNumber, String apiKey) {
        var client = new MyEnergiClient(serialNumber.toString(), apiKey);
        try {
            var zappis = client.getStatus().stream()
                    .filter(statusResponse -> statusResponse.getZappi() != null).findFirst();
            if (zappis.isPresent() && !zappis.get().getZappi().isEmpty()) {
                var zappiSerialNumber = SerialNumber.from(zappis.get().getZappi().get(0).getSerialNumber());
                new MyEnergiClient(zappiSerialNumber.toString(), serialNumber.toString(), apiKey).getStatus();
                return Optional.of(zappiSerialNumber);
            }
            log.warn("Zappi device not found");
        } catch (ClientException e) {
            log.warn("Unexpected error " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<HubDetailsResponse> read(String userId) {
        var details = loginService.readCredentials(userId);
        return details.map(creds ->
            new HubDetailsResponse(creds.getSerialNumber().toString(), creds.getZappiSerialNumber().toString())
        );
    }
}
