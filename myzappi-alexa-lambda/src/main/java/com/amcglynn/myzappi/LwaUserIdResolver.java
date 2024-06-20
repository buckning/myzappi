package com.amcglynn.myzappi;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.myzappi.core.service.UserIdResolver;

public class LwaUserIdResolver implements UserIdResolver {

    private final String userId;

    public LwaUserIdResolver(LwaClient lwaClient, HandlerInput handlerInput) {
        var accessToken = handlerInput.getRequestEnvelope().getSession().getUser().getAccessToken();
        userId = lwaClient.getUserId(accessToken)
                .orElseThrow(() -> new UserNotLinkedException(handlerInput.getRequestEnvelope().getSession().getUser().getUserId(),
                        handlerInput.getRequestEnvelope().getSession().getApplication().getApplicationId()));
    }

    @Override
    public String getUserId() {
        return userId;
    }
}
