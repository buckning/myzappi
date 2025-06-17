package com.amcglynn.myzappi.api.service;

import com.amcglynn.myenergi.MyEnergiClientFactory;
import com.amcglynn.myenergi.MyEnergiOAuthClient;
import com.amcglynn.myenergi.apiresponse.MyEnergiDeviceStatus;
import com.amcglynn.myenergi.apiresponse.StatusResponse;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myzappi.api.rest.response.AccountSummaryResponse;
import com.amcglynn.myzappi.core.dal.DevicesRepository;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.EmailAddress;
import com.amcglynn.myzappi.core.model.LibbiDevice;
import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.api.rest.ServerException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class RegistrationService {

    private final LoginService loginService;
    private final DevicesRepository devicesRepository;
    private final MyEnergiClientFactory myEnergiClientFactory;

    public RegistrationService(LoginService loginService, DevicesRepository devicesRepository, MyEnergiClientFactory myEnergiClientFactory) {
        this.loginService = loginService;
        this.devicesRepository = devicesRepository;
        this.myEnergiClientFactory = myEnergiClientFactory;
    }

    public void register(UserId userId, SerialNumber serialNumber, String apiKey) {
        discoverAndRegisterDetails(userId, serialNumber, apiKey);
    }

    /**
     * Register a user's myenergi myaccount with the myzappi account. This is an optional step needed for
     * oauth authentication, used for libbi control and other future features.
     * @param userId
     * @param emailAddress
     * @param password
     */
    public void register(UserId userId, EmailAddress emailAddress, String password) {
        validateMyEnergiAccountCredentials(emailAddress, password);
        loginService.register(userId.toString(), emailAddress, password);
    }

    private void validateMyEnergiAccountCredentials(EmailAddress emailAddress, String password) {
        MyEnergiOAuthClient oauthClient;
        try {
            oauthClient = myEnergiClientFactory.newMyEnergiOAuthClient(emailAddress.toString(), password);
            oauthClient.getUserHubsAndDevices();
        } catch (Exception e) {
            log.info("Invalid username or password");
            throw new ServerException(400);
        }
    }

    public void delete(UserId userId) {
        log.info("Deleting hub details for user = {}", userId);
        loginService.delete(userId.toString());
        devicesRepository.delete(userId);
        loginService.deleteMyEnergiAccountCredentials(userId);
    }

    private void discoverAndRegisterDetails(UserId userId, SerialNumber serialNumber, String apiKey) {
        var myEnergiDevices = discoverMyEnergiDevices(serialNumber, apiKey);

        var devices = getMyEnergiDevices(myEnergiDevices);

        if (devices.isEmpty()) {
            log.warn("Could not find Zappi for system");
            throw new ServerException(409);
        }
        log.info("Registering deployment details for user = {} hub = {} devices = {}", userId, serialNumber, devices);
        loginService.register(userId.toString(),
                serialNumber,
                apiKey,
                devices);
    }

    private void refreshDetails(UserId userId, SerialNumber serialNumber, String apiKey) {
        var myEnergiDevices = discoverMyEnergiDevices(serialNumber, apiKey);
        var devices = getMyEnergiDevices(myEnergiDevices);

        log.info("Refreshing deployment details for user = {} devices = {}", userId, devices);
        loginService.refreshDeploymentDetails(userId, devices);
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

    private List<MyEnergiDevice> getMyEnergiDevices(List<StatusResponse> statusResponse) {
        return statusResponse.stream()
                .map(this::getDevices)
                .flatMap(List::stream)
                .toList();
    }

    private List<MyEnergiDevice> getDevices(StatusResponse statusResponse) {
        if (statusResponse.getZappi() != null) {
            return statusResponse.getZappi()
                    .stream()
                    .map(this::toZappiDevice)
                    .toList();
        }
        if (statusResponse.getEddi() != null) {
            return statusResponse.getEddi()
                    .stream()
                    .map(this::toEddiDevice)
                    .toList();
        }
        if (statusResponse.getLibbi() != null) {
            return statusResponse.getLibbi()
                    .stream()
                    .map(this::toLibbiDevice)
                    .toList();
        }
        return List.of();
    }

    private MyEnergiDevice toZappiDevice(MyEnergiDeviceStatus status) {
        return new ZappiDevice(SerialNumber.from(status.getSerialNumber()));
    }

    private MyEnergiDevice toEddiDevice(MyEnergiDeviceStatus status) {
        return new EddiDevice(SerialNumber.from(status.getSerialNumber()), status.getTank1Name(), status.getTank2Name());
    }

    private MyEnergiDevice toLibbiDevice(MyEnergiDeviceStatus status) {
        return new LibbiDevice(SerialNumber.from(status.getSerialNumber()));
    }

    public List<MyEnergiDevice> readDevices(UserId userId) {
        return devicesRepository.read(userId);
    }

    public Optional<MyEnergiDevice> getDevice(UserId userId, SerialNumber serialNumber) {
        return devicesRepository.read(userId)
                .stream()
                .filter(device1 -> device1.getSerialNumber().equals(serialNumber))
                .findFirst();
    }

    public void refreshDeploymentDetails(UserId userId) {
        var creds = loginService.readCredentials(userId);
        if (creds.isEmpty()) {
            log.info("User is not registered {}", userId);
            throw new ServerException(404);
        }
        refreshDetails(userId, creds.get().getSerialNumber(), creds.get().getApiKey());
    }

    public AccountSummaryResponse getAccountSummary(UserId userId) {
        var creds = loginService.readCredentials(userId);
        var myEnergiAccount = loginService.readMyEnergiAccountCredentials(userId);
        return AccountSummaryResponse.builder()
                .hubRegistered(creds.isPresent())
                .myaccountRegistered(myEnergiAccount.isPresent())
                .build();
    }
}
