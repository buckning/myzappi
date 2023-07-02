package com.amcglynn.myzappi.login;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.myzappi.core.service.EncryptionService;

import java.net.HttpCookie;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class SessionManagementService {

    private SessionRepository sessionRepository;
    private EncryptionService encryptionService;
    private LwaClientFactory lwaClientFactory;
    private Supplier<Instant> instantSupplier;

    public SessionManagementService(SessionRepository sessionRepository, EncryptionService encryptionService,
                                    LwaClientFactory lwaClientFactory) {
        this.sessionRepository = sessionRepository;
        this.encryptionService = encryptionService;
        this.lwaClientFactory = lwaClientFactory;
        this.instantSupplier = Instant::now;
    }

    void setInstantSupplier(Supplier<Instant> instantSupplier) {
        this.instantSupplier = instantSupplier;
    }

    public Optional<Session> handle(APIGatewayProxyRequestEvent input, APIGatewayProxyResponseEvent response) {
        var session = getSession(input);

        if (session.isPresent()) {
            System.out.println("Found session with sessionID = " + session.get().getSessionId());
            return session;
        }

        session = attemptLogin(input);

        if (session.isPresent()) {
            var newSession = session.get();
            sessionRepository.write(newSession);

            System.out.println("Creating cookie with sessionID = " + newSession.getSessionId());
            var responseHeaders = new HashMap<>(response.getHeaders());
            responseHeaders.put("Set-Cookie", "sessionID=" + newSession.getSessionId() + "; Max-Age=" + newSession.getTtl() + "; Path=/; Secure; HttpOnly");
            response.setHeaders(responseHeaders);
        }
        return session;
    }

    private Optional<Session> attemptLogin(APIGatewayProxyRequestEvent input) {
        var map = input.getQueryStringParameters();

        if (map == null) {
            return Optional.empty();
        }

        var accessToken = map.get("access_token");
        var expiresIn = map.get("expires_in");

        if (accessToken == null || expiresIn == null) {
            return Optional.empty();
        }

        var sessionId = UUID.randomUUID().toString();

        var lwaClient = lwaClientFactory.newLwaClient();
        var userId = lwaClient.getUserId(accessToken);

        if (userId.isEmpty()) {
            return Optional.empty();
        }

        var encryptedToken = encryptionService.encrypt(accessToken);
        var expiryTimestamp = instantSupplier.get().plus(Long.parseLong(expiresIn), ChronoUnit.SECONDS);
        return Optional.of(new Session(sessionId, userId.get(), encryptedToken, expiryTimestamp.getEpochSecond()));
    }

    private Optional<Session> getSession(APIGatewayProxyRequestEvent input) {
        var headers = input.getHeaders();
        var sessionIdCookie = getSessionIdFromCookie(headers);

        if (sessionIdCookie.isEmpty()) {
            return Optional.empty();
        }

        // TODO - validate the cookie is still valid, if not delete the row
        return sessionRepository.read(sessionIdCookie.get().getValue());
    }

    private Optional<HttpCookie> getSessionIdFromCookie(Map<String, String> headers) {
        var cookieHeader = headers.get("cookie");

        if (cookieHeader == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookieHeader.split(";"))
                .map(cookie -> HttpCookie.parse(cookie).get(0))
                .filter(val -> "sessionID".equals(val.getName()))
                .findFirst();
    }

    public void invalidateSession(Session session) {
        sessionRepository.delete(session);
    }
}
