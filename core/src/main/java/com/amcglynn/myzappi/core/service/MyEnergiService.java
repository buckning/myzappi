package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.MockMyEnergiClient;
import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.MyEnergiClientFactory;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.exception.UserNotLoggedInException;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.LibbiDevice;
import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class MyEnergiService {

    private ZappiService zappiService;
    private EddiService eddiService;
    private LibbiService libbiService;
    private final MyEnergiClient client;

    private MyEnergiService(LoginService loginService, String user) {
        var userId = UserId.from(user);
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
                .ifPresentOrElse(serialNumber -> libbiService = new LibbiService(client, new MyEnergiClientFactory(), loginService), () -> libbiService = null);
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
