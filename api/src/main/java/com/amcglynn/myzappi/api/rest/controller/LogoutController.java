package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.service.AuthenticationService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
public class LogoutController {

    private final AuthenticationService authenticationService;

    public LogoutController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @SneakyThrows
    public Response logout(Request request) {
        var headers = new HashMap<String, String>();
        var sessionIdFromCookie = authenticationService.getSessionIdFromCookie(request.getHeaders());
        if (sessionIdFromCookie.isPresent()) {
            authenticationService.invalidateSession(sessionIdFromCookie.get());
            headers.put("Set-Cookie", "sessionID=" + sessionIdFromCookie.get() + ";expires=Thu, Jan 01 1970 00:00:00 UTC; Path=/; Secure; SameSite=None; HttpOnly; domain=.myzappiunofficial.com");
        }
        return new Response(200, headers);
    }
}
