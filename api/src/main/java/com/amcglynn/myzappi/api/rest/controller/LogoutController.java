package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.CompleteLoginRequest;
import com.amcglynn.myzappi.api.Session;
import com.amcglynn.myzappi.api.SessionId;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.service.AuthenticationService;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class LogoutController implements RestController {

    private AuthenticationService authenticationService;

    public LogoutController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public Response handle(Request request) {
        if (request.getMethod() == RequestMethod.GET) {
            return get(request);
        }
        log.info("Unsupported method for logout - {}", request.getMethod());
        throw new ServerException(404);
    }

    @SneakyThrows
    private Response get(Request request) {
        var headers = new HashMap<String, String>();
        var sessionIdFromCookie = authenticationService.getSessionIdFromCookie(request.getHeaders());
        if (sessionIdFromCookie.isPresent()) {
            authenticationService.invalidateSession(sessionIdFromCookie.get());
            headers.put("Set-Cookie", "sessionID=" + sessionIdFromCookie.get() + ";expires=Thu, Jan 01 1970 00:00:00 UTC; Path=/; Secure; SameSite=None; HttpOnly; domain=.myzappiunofficial.com");
        }
        return new Response(200, headers);
    }
}
