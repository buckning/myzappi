package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.MockMyEnergiClient;
import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.MyEnergiClientFactory;
import com.amcglynn.myenergi.apiresponse.StatusResponse;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.exception.UserNotLoggedInException;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.AutomationSnapshot;
import com.amcglynn.myzappi.core.model.EnergyStatus;
import com.amcglynn.myzappi.core.model.LibbiDevice;
import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import com.amcglynn.myzappi.core.service.automation.AutomationSnapshotMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class MyEnergiService {

    private ZappiService zappiService;
    private EddiService eddiService;
    private LibbiService libbiService;
    private final MyEnergiClient client;
    @Getter
    private final UserId userId;

    private MyEnergiService(LoginService loginService, String user) {
        userId = UserId.from(user);
        var creds = loginService.readCredentials(userId);
        var devices = loginService.readDevices(userId);
        if (creds.isEmpty()) {
            throw new UserNotLoggedInException(user);
        }

        var decryptedApiKey = creds.get().getApiKey();
        var hubSerialNumber = creds.get().getSerialNumber().toString();
        // find zappi serial number from devices
        var zappiSerialNumber = getZappiSerialNumber(devices);
        var eddiSerialNumberOpt = getEddiSerialNumber(devices);
        var libbiSerialNumberOpt = getLibbiSerialNumber(devices);

        if ("12345678".equals(hubSerialNumber) && "myDemoApiKey".equals(decryptedApiKey)) {
            client = new MockMyEnergiClient();
        } else{
            client = new MyEnergiClient(
                    zappiSerialNumber.map(SerialNumber::toString).orElse(null),
                    hubSerialNumber,
                    eddiSerialNumberOpt.map(SerialNumber::toString).orElse(null),
                    decryptedApiKey);
        }

        eddiSerialNumberOpt
                .ifPresentOrElse(serialNumber -> eddiService = new EddiService(client), () -> eddiService = null);
        zappiSerialNumber
                .ifPresentOrElse(serialNumber -> zappiService = new ZappiService(client), () -> zappiService = null);
        libbiSerialNumberOpt
                .ifPresentOrElse(serialNumber -> libbiService = new LibbiService(client, new MyEnergiClientFactory(),
                        loginService, getLibbiSerialNumbers(devices)), () -> libbiService = null);
    }

    public EnergyStatus getEnergyStatus() {
        return client.getStatus().stream()
                .map(this::toEnergyStatus)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(this::missingDeviceException);
    }

    public AutomationSnapshot getAutomationSnapshot() {
        return new AutomationSnapshotMapper().from(client.getStatus());
    }

    private MissingDeviceException missingDeviceException() {
        log.info("No Zappi, Eddi or Libbi device found");
        return new MissingDeviceException("No Zappi, Eddi or Libbi device found");
    }

    private Optional<EnergyStatus> toEnergyStatus(StatusResponse statusResponse) {
        if (statusResponse.getZappi() != null && !statusResponse.getZappi().isEmpty()) {
            return Optional.of(new EnergyStatus(statusResponse.getZappi().get(0)));
        }
        if (statusResponse.getEddi() != null && !statusResponse.getEddi().isEmpty()) {
            return Optional.of(new EnergyStatus(statusResponse.getEddi().get(0)));
        }
        if (statusResponse.getLibbi() != null && !statusResponse.getLibbi().isEmpty()) {
            return Optional.of(new EnergyStatus(statusResponse.getLibbi().get(0)));
        }
        return Optional.empty();
    }

    private Optional<SerialNumber> getEddiSerialNumber(List<MyEnergiDevice> devices) {
        return devices.stream()
                .filter(EddiDevice.class::isInstance)
                .findFirst()
                .map(MyEnergiDevice::getSerialNumber);
    }

    private Optional<SerialNumber> getZappiSerialNumber(List<MyEnergiDevice> devices) {
        return devices.stream()
                .filter(ZappiDevice.class::isInstance)
                .findFirst()
                .map(MyEnergiDevice::getSerialNumber);
    }

    private Optional<SerialNumber> getLibbiSerialNumber(List<MyEnergiDevice> devices) {
        return devices.stream()
                .filter(LibbiDevice.class::isInstance)
                .findFirst()
                .map(MyEnergiDevice::getSerialNumber);
    }

    private List<SerialNumber> getLibbiSerialNumbers(List<MyEnergiDevice> devices) {
        return devices.stream()
                .filter(LibbiDevice.class::isInstance)
                .map(MyEnergiDevice::getSerialNumber)
                .toList();
    }

    public Optional<ZappiService> getZappiService() {
        return Optional.ofNullable(zappiService);
    }

    public Optional<LibbiService> getLibbiService() {
        return Optional.ofNullable(libbiService);
    }

    public ZappiService getZappiServiceOrThrow() {
        if (zappiService == null) {
            throw new MissingDeviceException("Zappi service not available");
        }
        return zappiService;
    }

    public LibbiService getLibbiServiceOrThrow() {
        if (libbiService == null) {
            throw new MissingDeviceException("Libbi service not available");
        }
        return libbiService;
    }

    public EddiService getEddiServiceOrThrow() {
        if (eddiService == null) {
            throw new MissingDeviceException("Eddi service not available");
        }
        return eddiService;
    }

    public Optional<EddiService> getEddiService() {
        return Optional.ofNullable(eddiService);
    }

    public static class Builder {
        private final LoginService loginService;

        public Builder(LoginService loginService) {
            this.loginService = loginService;
        }

        public MyEnergiService build(UserIdResolver userIdResolver) {
            return new MyEnergiService(loginService, userIdResolver.getUserId());
        }
    }
}
