package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.LibbiMode;
import com.amcglynn.myenergi.LibbiState;
import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.MyEnergiClientFactory;
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

    private static final KiloWattHour USABLE_LIBBI_SIZE = new KiloWattHour(4.6);
    private static final KiloWattHour[] LIBBI_USABLE_SIZES = {
            new KiloWattHour(4.6),
            new KiloWattHour(9.2),
            new KiloWattHour(13.8),
            new KiloWattHour(18.4)
    };

    public LibbiService(MyEnergiClient client, MyEnergiClientFactory clientFactory, LoginService loginService,
                        List<SerialNumber> serialNumbers) {
        this.client = client;
        this.loginService = loginService;
        this.clientFactory = clientFactory;
        this.serialNumbers = serialNumbers;
        this.targetDeviceResolver = new AscendingOrderTargetDeviceResolver();
    }

    /**
     * This is going on some assumptions and not tested. Each Libbi has a usable energy size of 4.6kWh. Libbi's can be
     * combined with up to 4 Libbi's. This means the usable energy size can be 4.6kWh, 9.2kWh, 13.8kWh or 18.4kWh.
     * @param serialNumber the serial number of the Libbi
     * @return the usable energy size of the Libbi
     */
    public KiloWattHour getUsableEnergy(SerialNumber serialNumber) {
        var libbiStatus = client.getLibbiStatus(serialNumber.toString()).getLibbi().get(0);
        return getUsableEnergy(libbiStatus);
    }

    public KiloWattHour getUsableEnergy(com.amcglynn.myenergi.apiresponse.LibbiStatus libbiStatus) {
        var numberOfLibbis = (int) Math.floor(libbiStatus.getBatterySizeWh() / (USABLE_LIBBI_SIZE.getDouble() * 1000));
        return LIBBI_USABLE_SIZES[numberOfLibbis - 1];
    }

    public void setMode(LibbiMode libbiMode) {
        setMode(targetDeviceResolver.resolveTargetDevice(serialNumbers), libbiMode);
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

    public void setChargeFromGrid(MyEnergiAccountCredentials creds, boolean chargeFromGrid) {
        var serialNumber = targetDeviceResolver.resolveTargetDevice(serialNumbers);

        log.info("Setting charge from grid for serial number {} to {}", serialNumber, chargeFromGrid);
        clientFactory.newMyEnergiOAuthClient(creds.getEmailAddress(), creds.getPassword())
                .setChargeFromGrid(serialNumber.toString(), chargeFromGrid);
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

    public void setChargeTargetScaled(UserId userId, SerialNumber serialNumber, int targetEnergy) {
        var creds = loginService.readMyEnergiAccountCredentials(userId);

        creds.ifPresent(cred -> {
            var libbiStatus = client.getLibbiStatus(serialNumber.toString()).getLibbi().get(0);
            var batterySize = getUsableEnergy(libbiStatus);
            var targetEnergyWh = (int) (batterySize.getDouble() * 1000) * targetEnergy / 100;

            log.info("Setting target energy for serial number {} to {}% {}/{}", serialNumber, targetEnergy, targetEnergyWh, batterySize);
            clientFactory.newMyEnergiOAuthClient(cred.getEmailAddress(), cred.getPassword())
                    .setTargetEnergy(serialNumber.toString(), targetEnergyWh);
        });
    }

    public void setChargeTarget(UserId userId, int targetEnergy) {
        var serialNumber = targetDeviceResolver.resolveTargetDevice(serialNumbers);
        setChargeTarget(userId, serialNumber, targetEnergy);
    }

    public MyEnergiAccountCredentials validateMyEnergiAccountIsConfigured(UserId userId) {
        var creds = loginService.readMyEnergiAccountCredentials(userId);
        if (creds.isEmpty()) {
            throw new MyEnergiCredentialsNotConfiguredException("No MyEnergi account configured");
        }
        return creds.get();
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
