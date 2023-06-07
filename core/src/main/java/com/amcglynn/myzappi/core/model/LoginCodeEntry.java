package com.amcglynn.myzappi.core.model;

import java.time.Instant;

public class LoginCodeEntry {

    private final String code;
    private final String userId;
    private final Instant created;

    public LoginCodeEntry(String code, String userId, Instant created) {
        this.userId = userId;
        this.code = code;
        this.created = created;
    }

    public String getUserId() {
        return userId;
    }

    public String getCode() {
        return code;
    }

    public Instant getCreated() {
        return created;
    }
}
