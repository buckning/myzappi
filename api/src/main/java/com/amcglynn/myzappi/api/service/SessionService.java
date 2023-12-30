package com.amcglynn.myzappi.api.service;

import com.amcglynn.myzappi.api.Session;
import com.amcglynn.myzappi.api.SessionId;
import com.amcglynn.myzappi.api.SessionRepository;
import com.amcglynn.myzappi.core.model.UserId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class SessionService {

    private static final long DEFAULT_SESSION_TTL = 3600;

    private SessionRepository sessionRepository;
    private Supplier<Instant> instantSupplier;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
        this.instantSupplier = Instant::now;
    }

    void setInstantSupplier(Supplier<Instant> instantSupplier) {
        this.instantSupplier = instantSupplier;
    }

    public Session createSession(UserId userId) {
        var sessionId = SessionId.from(UUID.randomUUID().toString());
        var expiryTimestamp = instantSupplier.get().plus(DEFAULT_SESSION_TTL, ChronoUnit.SECONDS);
        var session = new Session(sessionId, userId, expiryTimestamp.getEpochSecond());
        sessionRepository.write(session);
        return session;
    }

    private long now() {
        return instantSupplier.get().getEpochSecond();
    }

    public Optional<Session> getValidSession(SessionId sessionId) {
        var session = sessionRepository.read(sessionId);
        if (session.isPresent() && session.get().getTtl() > now()) {
            return session;
        }
        // session is expired, it will be auto deleted by DynamoDB
        return Optional.empty();
    }

    public void invalidateSession(Session session) {
        sessionRepository.delete(session);
    }
}
