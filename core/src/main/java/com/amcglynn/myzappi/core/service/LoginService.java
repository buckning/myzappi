package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.LoginCodeRepository;
import com.amcglynn.myzappi.core.model.LoginResponse;
import com.amcglynn.myzappi.core.model.LoginState;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.ZappiCredentials;

public class LoginService {

    private final CredentialsRepository credentialsRepository;
    private final LoginCodeRepository loginCodeRepository;

    public LoginService(CredentialsRepository credentialsRepository, LoginCodeRepository loginCodeRepository) {
        this.credentialsRepository = credentialsRepository;
        this.loginCodeRepository = loginCodeRepository;
    }

    public LoginResponse login(String user, SerialNumber serialNumber) {
        var credsOptional = credentialsRepository.read(user);

        if (credsOptional.isEmpty()) {
            return handleNewLogin(user, serialNumber);
        }

        var creds = credsOptional.get();
        if (isAlreadyLoggedIn(creds)) {
            return new LoginResponse(creds, LoginState.LOGIN_COMPLETE);
        }

        return new LoginResponse(creds, LoginState.GENERATED_LOGIN_CODE);
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
