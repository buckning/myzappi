package com.amcglynn.myzappi;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.service.ZappiService;

import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

    public static Future<List<ZappiStatusSummary>> getZappiStatusSummary(HandlerInput handlerInput) {
        var requestAttributes = handlerInput.getAttributesManager().getRequestAttributes();
        return (Future<List<ZappiStatusSummary>>) requestAttributes.get("zappiStatusSummary");
    }

    public static ZappiStatusSummary waitForZappiStatusSummary(HandlerInput handlerInput) throws ExecutionException, InterruptedException {
        return getZappiStatusSummary(handlerInput).get().get(0);
    }

    public static List<ZappiHistory> waitForHistory(HandlerInput handlerInput) throws ExecutionException, InterruptedException {
        var requestAttributes = handlerInput.getAttributesManager().getRequestAttributes();
        return ((Future<List<ZappiHistory>>) requestAttributes.get("zappiHistory")).get();
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
