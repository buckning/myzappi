package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.ZappiCredentials;

import java.util.Optional;

public class LoginService {

    private final CredentialsRepository credentialsRepository;
    private final EncryptionService encryptionService;

    public LoginService(CredentialsRepository credentialsRepository,
                        EncryptionService encryptionService) {
        this.credentialsRepository = credentialsRepository;
        this.encryptionService = encryptionService;
    }

    public void logout(String user) {
        var creds = credentialsRepository.read(user);
        creds.ifPresent(credentials -> credentialsRepository.delete(credentials.getUserId()));
    }

    public void register(String userId, SerialNumber zappiSerialNumber, SerialNumber serialNumber, String apiKey) {
        var encryptedKey = encryptionService.encrypt(apiKey);
        var newCreds = new ZappiCredentials(userId,
                zappiSerialNumber,
                serialNumber,
                encryptedKey);

        credentialsRepository.delete(userId);
        credentialsRepository.write(newCreds);
    }

    public Optional<ZappiCredentials> readCredentials(String userId) {
        return credentialsRepository.read(userId);
    }

    public void delete(String userId) {
        credentialsRepository.delete(userId);
    }
}
