package com.amcglynn.myzappi.api;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class SessionId {
    private final String id;

    public static SessionId from(String userId) {
        return new SessionId(userId);
    }

    private SessionId(String userId) {
        this.id = userId;
    }

    @Override
    public String toString() {
        return id;
    }
}
