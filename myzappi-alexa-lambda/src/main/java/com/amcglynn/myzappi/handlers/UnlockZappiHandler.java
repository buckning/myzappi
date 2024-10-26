package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.core.Brand;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;

@Slf4j
public class UnlockZappiHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("UnlockZappi"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        getZappiServiceOrThrow(handlerInput).unlockZappi();

        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "unlocking-charger"))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "unlocking-charger"))
                .withShouldEndSession(false)
                .build();
    }
}
