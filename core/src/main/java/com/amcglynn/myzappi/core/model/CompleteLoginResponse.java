package com.amcglynn.myzappi.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor
public class CompleteLoginResponse {
    @Getter
    private final CompleteLoginState state;
    private final ZappiCredentials creds;

    public CompleteLoginResponse(CompleteLoginState state) {
        this.state = state;
        this.creds = null;
    }

    public Optional<ZappiCredentials> getZappiCredentials() {
        return Optional.ofNullable(creds);
    }
}
