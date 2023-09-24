package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.SessionManagementService;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
public class LogoutController implements RestController {

    private final SessionManagementService sessionManagementService;

    public LogoutController(SessionManagementService sessionManagementService) {
        this.sessionManagementService = sessionManagementService;
    }

    @Override
    public Response handle(Request request) {
        var session = request.getSession();
        var responseHeaders = new HashMap<String, String>();

        session.ifPresent(s -> {
            log.info("Invalidating session for {}", s.getUserId());
            sessionManagementService.invalidateSession(s);
            responseHeaders.put("Set-Cookie", "sessionID=" + s.getSessionId() + "; expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; Secure; HttpOnly");
        });
        responseHeaders.put("Location", "https://myzappiunofficial.com");
        return new Response(302, responseHeaders);
    }
}
