package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.service.TokenService;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticates the LWA token and creates a session
 */
@Slf4j
public class AuthenticateController {

    private TokenService tokenService;

    public AuthenticateController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public boolean isAuthenticated(Request request, String lwaToken) {
        var tokenInfo = tokenService.getTokenInfo(lwaToken);
        if (tokenInfo.isPresent()) {
            request.setUserId(tokenInfo.get().getUserId());
        }
        return tokenInfo.isPresent();
    }

}
