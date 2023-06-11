package com.amcglynn.myzappi.core.model;

import java.time.Instant;

public class LoginCodeEntry {

    private final LoginCode code;
    private final String userId;
    private final Instant created;

    public LoginCodeEntry(LoginCode code, String userId, Instant created) {
        this.userId = userId;
        this.code = code;
        this.created = created;
    }

    public String getUserId() {
        return userId;
    }

    public LoginCode getCode() {
        return code;
    }

    public Instant getCreated() {
        return created;
    }
}
