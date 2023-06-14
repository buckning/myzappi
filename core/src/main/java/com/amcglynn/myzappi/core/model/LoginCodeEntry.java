package com.amcglynn.myzappi.core.model;

import java.time.Instant;

public class LoginCodeEntry {

    private final LoginCode code;
    private final String userId;
    private final SerialNumber serialNumber;
    private final Instant created;

    public LoginCodeEntry(LoginCode code, String userId, SerialNumber serialNumber, Instant created) {
        this.userId = userId;
        this.code = code;
        this.serialNumber = serialNumber;
        this.created = created;
    }

    public String getUserId() {
        return userId;
    }

    public LoginCode getCode() {
        return code;
    }

    public SerialNumber getSerialNumber() {
        return serialNumber;
    }

    public Instant getCreated() {
        return created;
    }
}
