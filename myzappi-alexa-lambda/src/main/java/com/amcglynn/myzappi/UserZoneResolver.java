package com.amcglynn.myzappi;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amcglynn.lwa.LwaClient;

import java.time.ZoneId;

/**
 * Retrieve ZoneId for the specific device. This is done by getting the base url and access token from the request
 * end then calling the API to get the timezone for the device.
 */
public class UserZoneResolver {

    private final LwaClient lwaClient;

    public UserZoneResolver(LwaClient lwaClient) {
        this.lwaClient = lwaClient;
    }

    public ZoneId getZoneId(HandlerInput handlerInput) {
        var zoneId = lwaClient.getTimeZone(handlerInput.getRequestEnvelope().getContext().getSystem().getApiEndpoint(),
                        handlerInput.getRequestEnvelope().getContext().getSystem().getDevice().getDeviceId(),
                        handlerInput.getRequestEnvelope().getContext().getSystem().getApiAccessToken())
                .orElse("Europe/London");
        return ZoneId.of(zoneId);
    }
}
