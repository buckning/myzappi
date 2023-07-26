package com.amcglynn.myzappi.login.rest;

import com.amcglynn.myzappi.login.AuthenticateRequest;
import com.amcglynn.myzappi.login.Session;
import com.amcglynn.myzappi.login.SessionManagementService;
import com.amcglynn.myzappi.login.service.TokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

/**
 * Authenticates the LWA token and creates a session
 */
@Slf4j
public class AuthenticateController implements RestController {

    private TokenService tokenService;
    private SessionManagementService sessionManagementService;

    public AuthenticateController(TokenService tokenService, SessionManagementService sessionManagementService) {
        this.tokenService = tokenService;
        this.sessionManagementService = sessionManagementService;
    }

    @Override
    public Response handle(Request request) {
        if (request.getMethod() == RequestMethod.POST) {
            var session = authenticate(request);
            var responseHeaders = new HashMap<String, String>();
            responseHeaders.put("Set-Cookie", "sessionID=" + session.getSessionId() + "; Max-Age=" + session.getTtl() + "; Path=/; Secure; HttpOnly");
            return new Response(200, responseHeaders);
        }
        log.info("Unsupported method for authenticate - " + request.getMethod());
        throw new ServerException(404);
    }

    private Session authenticate(Request request) {
        if (request.getBody() == null) {
            log.info("Null body in POST request");
            throw new ServerException(400);
        }
        try {
            var body = new ObjectMapper().readValue(request.getBody(), new TypeReference<AuthenticateRequest>() {
            });

            if (body.getAccessToken() == null) {
                log.info("Null accessToken in POST request");
                throw new ServerException(400);
            }
            var accessTokenFromRequest = body.getAccessToken().trim();

            var tokenInfo = tokenService.getTokenInfo(accessTokenFromRequest);
            if (tokenInfo.isPresent()) {
                return sessionManagementService.createSession(tokenInfo.get().getUserId(), accessTokenFromRequest, tokenInfo.get().getExpires());
            }
            log.info("Could not create session from token provided");
            throw new ServerException(403);
        } catch (JsonProcessingException e) {
            log.info("Invalid request");
            throw new ServerException(400);
        }
    }

}
