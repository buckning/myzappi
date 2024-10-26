package com.amcglynn.myzappi;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.service.ZappiService;

import java.time.ZoneId;

public class RequestAttributes {
    private RequestAttributes() {
    }

    public static ZoneId getZoneId(HandlerInput handlerInput) {
        var requestAttributes = handlerInput.getAttributesManager().getRequestAttributes();
        return (ZoneId) requestAttributes.get("zoneId");
    }

    public static String getUserId(HandlerInput handlerInput) {
        var requestAttributes = handlerInput.getAttributesManager().getRequestAttributes();
        return (String) requestAttributes.get("userId");
    }

    public static ZappiService getZappiServiceOrThrow(HandlerInput handlerInput) {
        var requestAttributes = handlerInput.getAttributesManager().getRequestAttributes();
        var zappiService = (ZappiService) requestAttributes.get("zappiService");
        if (zappiService == null) {
            throw new MissingDeviceException("Zappi service not available");
        }
        return zappiService;
    }
}
