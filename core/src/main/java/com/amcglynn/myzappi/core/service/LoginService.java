package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.LoginCodeRepository;
import com.amcglynn.myzappi.core.model.CompleteLoginResponse;
import com.amcglynn.myzappi.core.model.CompleteLoginState;
import com.amcglynn.myzappi.core.model.LoginCode;
import com.amcglynn.myzappi.core.model.LoginCodeEntry;
import com.amcglynn.myzappi.core.model.LoginResponse;
import com.amcglynn.myzappi.core.model.LoginState;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.ZappiCredentials;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class LoginService {

    private final CredentialsRepository credentialsRepository;
    private final LoginCodeRepository loginCodeRepository;
    private final EncryptionService encryptionService;

    public LoginService(CredentialsRepository credentialsRepository,
                        LoginCodeRepository loginCodeRepository,
                        EncryptionService encryptionService) {
        this.credentialsRepository = credentialsRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.encryptionService = encryptionService;
    }

    public LoginResponse login(String user, SerialNumber serialNumber) {
        var credsOptional = credentialsRepository.read(user);

        if (credsOptional.isEmpty()) {
            return handleNewLogin(user, serialNumber);
        }

        return handleLoginForExistingUser(credsOptional.get());
    }

    public void logout(String user) {
        var creds = credentialsRepository.read(user);
        if (creds.isPresent()) {
            credentialsRepository.delete(creds.get().getUserId());
            loginCodeRepository.delete(creds.get().getCode());
        }
    }

    private LoginResponse handleLoginForExistingUser(ZappiCredentials creds) {
        if (isAlreadyLoggedIn(creds)) {
            return new LoginResponse(creds, LoginState.LOGIN_COMPLETE);
        }

        return handleExpiredCode(creds);
    }

    private LoginResponse handleExpiredCode(ZappiCredentials creds) {
        var existingLoginCode = loginCodeRepository.read(creds.getCode());

        if (existingLoginCode.isEmpty() || isExpired(existingLoginCode.get())) {
            credentialsRepository.delete(creds.getUserId());
            loginCodeRepository.delete(creds.getCode());
            delay();    // this should really be an update to the existing tables so this was just a quick hack instead.
            var response = handleNewLogin(creds.getUserId(), creds.getSerialNumber());
            return new LoginResponse(response.getCreds(), LoginState.RECREATED_NEW_CODE);
        }
        return new LoginResponse(creds, LoginState.EXISTING_LOGIN_CODE);
    }

    private void delay() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public CompleteLoginResponse completeLogin(LoginCode loginCode, String apiKey) {
        var encryptedKey = encryptionService.encrypt(apiKey);
        var response = consumeLoginCode(loginCode, encryptedKey);
        loginCodeRepository.delete(loginCode);
        return response;
    }

    private CompleteLoginResponse consumeLoginCode(LoginCode loginCode, ByteBuffer encryptedKey) {
        var loginCodeOptional = loginCodeRepository.read(loginCode);
        if (loginCodeOptional.isEmpty()) {
            return new CompleteLoginResponse(CompleteLoginState.LOGIN_CODE_NOT_FOUND);
        }

        if (isExpired(loginCodeOptional.get())) {
            return new CompleteLoginResponse(CompleteLoginState.LOGIN_CODE_EXPIRED);
        }

        var credsOptional = credentialsRepository.read(loginCodeOptional.get().getUserId());

        if (credsOptional.isEmpty() || !isValidLoginCode(loginCode, credsOptional.get())) {
            return new CompleteLoginResponse(CompleteLoginState.ASSOCIATED_USER_NOT_FOUND);
        }

        var oldCreds = credsOptional.get();

        var newCreds = new ZappiCredentials(oldCreds.getUserId(),
                oldCreds.getSerialNumber(),
                oldCreds.getCode(),
                encryptedKey);

        credentialsRepository.delete(oldCreds.getUserId());
        credentialsRepository.write(newCreds);

        return new CompleteLoginResponse(CompleteLoginState.COMPLETE, newCreds);
    }

    public Optional<ZappiCredentials> readCredentials(String userId) {
        return credentialsRepository.read(userId);
    }

    public boolean isLoggedIn(ZappiCredentials creds) {
        return creds.getEncryptedApiKey().isPresent();
    }

    private boolean isExpired(LoginCodeEntry loginCodeEntry) {
        return loginCodeEntry.getCreated().plus(Duration.ofDays(1)).isBefore(Instant.now());
    }

    private boolean isValidLoginCode(LoginCode loginCode, ZappiCredentials credentials) {
        return credentials.getCode().equals(loginCode);
    }

    private LoginResponse handleNewLogin(String user, SerialNumber serialNumber) {
        var code = new LoginCode();
        var creds = new ZappiCredentials(user, serialNumber, code);
        credentialsRepository.write(creds);
        loginCodeRepository.write(creds);
        return new LoginResponse(creds, LoginState.NEW);
    }

    private boolean isAlreadyLoggedIn(ZappiCredentials creds) {
        return creds.getEncryptedApiKey().isPresent();
    }
}
