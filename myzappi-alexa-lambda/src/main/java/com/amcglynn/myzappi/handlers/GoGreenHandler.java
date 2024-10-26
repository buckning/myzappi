package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.Brand;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;

public class GoGreenHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GoGreen"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = getZappiServiceOrThrow(handlerInput);
        var chargeMode = ZappiChargeMode.ECO_PLUS;
        zappiService.setChargeMode(chargeMode);
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", chargeMode.getDisplayName())))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", chargeMode.getDisplayName())))
                .withShouldEndSession(false)
                .build();
    }
}
