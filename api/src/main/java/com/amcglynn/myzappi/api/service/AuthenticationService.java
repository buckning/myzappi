package com.amcglynn.myzappi.api.service;

import com.amcglynn.lwa.TokenInfo;
import com.amcglynn.myzappi.api.Session;
import com.amcglynn.myzappi.api.SessionId;
import com.amcglynn.myzappi.api.rest.Request;
import lombok.extern.slf4j.Slf4j;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Authenticates based on the bearer token or a session. It expects to have either
 * 1. A session ID cookie, which must be in the session table in the DB
 * 2. A valid bearer token. Note that the bearer token must be inside the Authorization header and not in the URL. This token will be validated against the LWA service
 *
 * The session ID is preferred over the bearer token. If the session ID is not present, only then will the bearer token be validated.
 */
@Slf4j
public class AuthenticationService {

    private final TokenService tokenService;
    private final SessionService sessionService;

    public AuthenticationService(TokenService tokenService, SessionService sessionService) {
        this.tokenService = tokenService;
        this.sessionService = sessionService;
    }

    public Optional<Session> authenticate(Request request) {
        var sessionId = getSessionIdFromCookie(request.getHeaders());
        if (sessionId.isPresent()) {
            var session = sessionService.getValidSession(sessionId.get());
            if (session.isPresent()) {
                request.setUserId(session.get().getUserId().toString());
                return session;
            }
        }

        if (isValidBearerToken(request)) {
            return Optional.of(sessionService.createSession(request.getUserId()));
        }

        return Optional.empty();
    }

    public Optional<Session> authenticateLwaToken(Request request) {
        if (isValidBearerToken(request)) {
            return Optional.of(sessionService.createSession(request.getUserId()));
        }

        return Optional.empty();
    }

    public Optional<SessionId> getSessionIdFromCookie(Map<String, String> headers) {
        var cookieHeader = headers.get("cookie");

        if (cookieHeader == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookieHeader.split(";"))
                .map(cookie -> HttpCookie.parse(cookie).get(0))
                .filter(val -> "sessionID".equals(val.getName()))
                .map(HttpCookie::getValue)
                .map(SessionId::from)
                .findFirst();
    }

    public void invalidateSession(SessionId sessionId) {
        sessionService.getValidSession(sessionId).ifPresent(sessionService::invalidateSession);
    }

    private boolean validateAwsToken(Request request, String lwaToken) {
        var awsUser = getAwsUser(lwaToken);
        awsUser.ifPresent(request::setUserId);
        return awsUser.isPresent();
    }

    private Optional<String> getAwsUser(String lwaToken) {
        return tokenService.getTokenInfo(lwaToken).map(TokenInfo::getUserId);
    }

    private boolean isValidBearerToken(Request request) {
        var authorization = request.getHeaders().get("Authorization");

        if (authorization == null) {
            return false;
        }

        var tokens = authorization.split("Bearer ");
        if (tokens.length == 2) {
            return validateAwsToken(request, tokens[1]);
        }

        return false;
    }
}
