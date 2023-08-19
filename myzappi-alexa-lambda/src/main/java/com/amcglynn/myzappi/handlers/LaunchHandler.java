package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.LaunchRequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.core.Brand;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.requestType;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class LaunchHandler implements LaunchRequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput, LaunchRequest launchRequest) {
        return handlerInput.matches(requestType(LaunchRequest.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput, LaunchRequest launchRequest) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "help"))
                .withSimpleCard(Brand.NAME, "I can change your charge type and provide you energy usage. " +
                        "Ask me to start charging or to switch to solar. You can also ask me for an energy summary.")
                .withShouldEndSession(false)
                .build();
    }
}
