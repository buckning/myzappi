package com.amcglynn.myzappi.api;

import com.amcglynn.myzappi.core.model.UserId;

import java.time.Duration;

public class Session {

    public static final Duration DEFAULT_TTL = Duration.ofSeconds(3600); // 1 hour
    private final long ttl;
    private final UserId userId;
    private final SessionId sessionId;

    public Session(SessionId sessionId, UserId userId, long ttl) {
        this.ttl = ttl;
        this.userId = userId;
        this.sessionId = sessionId;
    }

    public long getTtl() {
        return ttl;
    }

    public UserId getUserId() {
        return userId;
    }

    public SessionId getSessionId() {
        return sessionId;
    }
}
