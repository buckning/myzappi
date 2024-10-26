package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ZappiService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;

@Slf4j
public class ChargeMyCarHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("ChargeMyCar"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var chargeMode = ZappiChargeMode.FAST;
        getZappiServiceOrThrow(handlerInput).setChargeMode(chargeMode);
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", chargeMode.getDisplayName())))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", chargeMode.getDisplayName())))
                .withShouldEndSession(false)
                .build();
    }
}
