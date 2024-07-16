package com.amcglynn.myzappi;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.myzappi.core.service.UserIdResolver;

public class UserIdResolverFactory {

    private LwaClient lwaClient;
    public UserIdResolverFactory(LwaClient lwaClient) {
        this.lwaClient = lwaClient;
    }

    public UserIdResolver newUserIdResolver(HandlerInput handlerInput) {
        var accessToken = handlerInput.getRequestEnvelope().getSession().getUser().getAccessToken();
        if (accessToken == null) {
            throw new UserNotLinkedException(handlerInput.getRequestEnvelope().getSession().getUser().getUserId(),
                    handlerInput.getRequestEnvelope().getSession().getApplication().getApplicationId());
        }
        return new LwaUserIdResolver(lwaClient, handlerInput);
    }
}
