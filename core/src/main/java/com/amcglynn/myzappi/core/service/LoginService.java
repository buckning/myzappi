package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.DevicesRepository;
import com.amcglynn.myzappi.core.model.HubCredentials;
import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.MyEnergiDeployment;
import com.amcglynn.myzappi.core.model.UserId;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class LoginService {

    private final CredentialsRepository credentialsRepository;
    private final DevicesRepository devicesRepository;
    private final EncryptionService encryptionService;

    public LoginService(CredentialsRepository credentialsRepository,
                        DevicesRepository devicesRepository,
                        EncryptionService encryptionService) {
        this.credentialsRepository = credentialsRepository;
        this.devicesRepository = devicesRepository;
        this.encryptionService = encryptionService;
    }

    public void register(String userId, SerialNumber serialNumber, String apiKey, List<MyEnergiDevice> devices) {
        var encryptedKey = encryptionService.encrypt(apiKey);
        var newCreds = new MyEnergiDeployment(userId,
                serialNumber,
                encryptedKey);

        credentialsRepository.delete(userId);
        credentialsRepository.write(newCreds);

        devicesRepository.write(UserId.from(userId), devices);
    }

    public void refreshDeploymentDetails(UserId userId, List<MyEnergiDevice> devices) {
        devicesRepository.write(userId, devices);
    }

    public List<MyEnergiDevice> readDevices(UserId userId) {
        return devicesRepository.read(userId);
    }

    public Optional<HubCredentials> readCredentials(UserId userId) {
        return credentialsRepository.read(userId.toString())
                .map(c -> new HubCredentials(c.getSerialNumber(),
                        encryptionService.decrypt(c.getEncryptedApiKey())));
    }

    public void delete(String userId) {
        credentialsRepository.delete(userId);
    }
}
