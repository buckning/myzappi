package com.amcglynn.myzappi.login.service;

import com.amcglynn.lwa.TokenInfo;
import com.amcglynn.myzappi.login.LwaClientFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TokenService {
    private LwaClientFactory lwaClientFactory;

    public TokenService(LwaClientFactory lwaClientFactory) {
        this.lwaClientFactory = lwaClientFactory;
    }

    public Optional<TokenInfo> getTokenInfo(String accessToken) {
        return lwaClientFactory.newLwaClient().getTokenInfo(accessToken);
    }
}
