package com.amcglynn.myzappi.interceptors;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor;
import com.amcglynn.myzappi.UserZoneResolver;

public class ZoneIdInjectorInterceptor implements RequestInterceptor {

    private final UserZoneResolver userZoneResolver;

    public ZoneIdInjectorInterceptor(UserZoneResolver userZoneResolver) {
        this.userZoneResolver = userZoneResolver;
    }

    @Override
    public void process(HandlerInput handlerInput) {
        var requestAttributes = handlerInput.getAttributesManager().getRequestAttributes();
        var userZoneId = userZoneResolver.getZoneId(handlerInput);
        requestAttributes.put("zoneId", userZoneId);
    }
}
