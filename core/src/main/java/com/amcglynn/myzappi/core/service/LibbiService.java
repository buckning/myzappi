package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.LibbiMode;
import com.amcglynn.myenergi.LibbiState;
import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.MyEnergiClientFactory;
import com.amcglynn.myenergi.MyEnergiOAuthClient;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.exception.MyEnergiCredentialsNotConfiguredException;
import com.amcglynn.myzappi.core.model.LibbiStatus;
import com.amcglynn.myzappi.core.model.MyEnergiAccountCredentials;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class LibbiService {

    private final MyEnergiClient client;
    private final LoginService loginService;
    private final MyEnergiClientFactory clientFactory;
    private final List<SerialNumber> serialNumbers;
    private final TargetDeviceResolver targetDeviceResolver;

    public LibbiService(MyEnergiClient client, MyEnergiClientFactory clientFactory, LoginService loginService,
                        List<SerialNumber> serialNumbers) {
        this.client = client;
        this.loginService = loginService;
        this.clientFactory = clientFactory;
        this.serialNumbers = serialNumbers;
        this.targetDeviceResolver = new AscendingOrderTargetDeviceResolver();
    }

    public void setMode(SerialNumber serialNumber, LibbiMode mode) {
        log.info("Setting libbi mode for serial number {} to {}", serialNumber, mode);
        client.setLibbiMode(serialNumber.toString(), mode);
    }

    public void setChargeFromGrid(UserId userId, SerialNumber serialNumber, boolean chargeFromGrid) {
        var creds = loginService.readMyEnergiAccountCredentials(userId);
        creds.ifPresent(cred -> {
            log.info("Setting charge from grid for serial number {} to {}", serialNumber, chargeFromGrid);
            clientFactory.newMyEnergiOAuthClient(cred.getEmailAddress(), cred.getPassword())
                            .setChargeFromGrid(serialNumber.toString(), chargeFromGrid);
        });
    }

    public void setChargeTarget(UserId userId, SerialNumber serialNumber, int targetEnergy) {
        var creds = loginService.readMyEnergiAccountCredentials(userId);

        creds.ifPresent(cred -> {
            var libbiStatus = client.getLibbiStatus(serialNumber.toString()).getLibbi().get(0);
            var batterySize = libbiStatus.getBatterySizeWh();
            var targetEnergyWh = batterySize * targetEnergy / 100;

            log.info("Setting target energy for serial number {} to {}% {}/{}", serialNumber, targetEnergy, targetEnergyWh, batterySize);
            clientFactory.newMyEnergiOAuthClient(cred.getEmailAddress(), cred.getPassword())
                            .setTargetEnergy(serialNumber.toString(), targetEnergyWh);
            });
    }

    public void validateMyEnergiAccountIsConfigured(UserId userId) {
        var creds = loginService.readMyEnergiAccountCredentials(userId);
        if (creds.isEmpty()) {
            throw new MyEnergiCredentialsNotConfiguredException("No MyEnergi account configured");
        }
    }

    public LibbiStatus getStatus(UserId userId) {
        return getStatus(userId, targetDeviceResolver.resolveTargetDevice(serialNumbers));
    }

    public LibbiStatus getStatus(UserId userId, SerialNumber serialNumber) {
        var creds = loginService.readMyEnergiAccountCredentials(userId);
        Boolean chargeFromGrid = null;
        KiloWattHour energyTarget = null;

        var libbiStatus = client.getLibbiStatus(serialNumber.toString()).getLibbi().get(0);

        if (creds.isPresent()) {
            var cred = creds.get();

            var libbiChargeSetup = getLibbiChargeSetup(cred, serialNumber);

            if (libbiChargeSetup.isPresent()) {
                chargeFromGrid = libbiChargeSetup.get().getChargeFromGridEnabled();
                energyTarget = libbiChargeSetup.get().getEnergyTargetKWh();
            }
        }
        return LibbiStatus.builder()
                .stateOfChargePercentage(libbiStatus.getStateOfCharge())
                .batterySizeKWh(new KiloWattHour(libbiStatus.getBatterySizeWh() / 1000.0))
                .serialNumber(serialNumber)
                .energyTargetKWh(energyTarget)
                .chargeFromGridEnabled(chargeFromGrid)
                .state(LibbiState.from(libbiStatus.getStatus()))
                .build();
    }

    private Optional<LibbiStatus> getLibbiChargeSetup(MyEnergiAccountCredentials cred, SerialNumber serialNumber) {
        try {
            log.info("Getting Libbi status for serial number {}", serialNumber);
            var response = clientFactory.newMyEnergiOAuthClient(cred.getEmailAddress(), cred.getPassword())
                    .getLibbiChargeSetup(serialNumber.toString());

            return Optional.of(LibbiStatus.builder()
                    .chargeFromGridEnabled(response.isChargeFromGrid())
                    .energyTargetKWh(new KiloWattHour(response.getEnergyTarget() / 1000.0))
                    .build());
        } catch (Exception e) {
            log.error("Error getting libbi charge setup for serial number {}", serialNumber, e);
            return Optional.empty();
        }
    }
}
