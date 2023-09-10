package com.amcglynn.myzappi.api;

import java.nio.ByteBuffer;

public class Session {
    private final long ttl;
    private final String userId;
    private final String sessionId;
    private final ByteBuffer encryptedToken;

    public Session(String sessionId, String userId, ByteBuffer encryptedToken, long ttl) {
        this.ttl = ttl;
        this.userId = userId;
        this.sessionId = sessionId;
        this.encryptedToken = encryptedToken;
    }

    public long getTtl() {
        return ttl;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public ByteBuffer getEncryptedToken() {
        return encryptedToken;
    }
}
