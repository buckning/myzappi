package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.DevicesRepository;
import com.amcglynn.myzappi.core.dal.MyEnergiAccountCredentialsRepository;
import com.amcglynn.myzappi.core.model.EmailAddress;
import com.amcglynn.myzappi.core.model.HubCredentials;
import com.amcglynn.myzappi.core.model.MyEnergiAccountCredentials;
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
    private final MyEnergiAccountCredentialsRepository myEnergiAccountCredentialsRepository;

    public LoginService(CredentialsRepository credentialsRepository,
                        DevicesRepository devicesRepository,
                        MyEnergiAccountCredentialsRepository myEnergiAccountCredentialsRepository,
                        EncryptionService encryptionService) {
        this.credentialsRepository = credentialsRepository;
        this.devicesRepository = devicesRepository;
        this.encryptionService = encryptionService;
        this.myEnergiAccountCredentialsRepository = myEnergiAccountCredentialsRepository;
    }

    /**
     * Register myenergi hub with the myzappi account. This is needed for general zappi and eddi control.
     * @param userId Amazon user ID
     * @param serialNumber myenergi hub serial number
     * @param apiKey myenergi API key
     * @param devices List of devices connected to the hub
     */
    public void register(String userId, SerialNumber serialNumber, String apiKey, List<MyEnergiDevice> devices) {
        var encryptedKey = encryptionService.encrypt(apiKey);
        var newCreds = new MyEnergiDeployment(userId,
                serialNumber,
                encryptedKey);

        credentialsRepository.delete(userId);
        credentialsRepository.write(newCreds);

        devicesRepository.write(UserId.from(userId), devices);
    }

    /**
     * Register myenergi myaccount email and password with the myzappi account. This is needed for oauth authentication
     * with myenergi APIs for libbi control and others. This is optional and only needed if the user wants to control
     * their libbi devices.
     * @param userId Amazon user ID
     * @param emailAddress myenergi myaccount email address
     * @param password myenergi myaccount password
     */
    public void register(String userId, EmailAddress emailAddress, String password) {
        var encryptedPassword = encryptionService.encrypt(password);
        var encryptedEmail = encryptionService.encrypt(emailAddress.toString());

        var creds = new MyEnergiAccountCredentials(userId, encryptedEmail, encryptedPassword);
        myEnergiAccountCredentialsRepository.write(creds);
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
