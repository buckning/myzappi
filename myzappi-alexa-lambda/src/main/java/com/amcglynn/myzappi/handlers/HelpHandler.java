package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.core.Brand;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amazon.ask.request.Predicates.requestType;

@Slf4j
public class HelpHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("AMAZON.HelpIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech("Hi, I can change your charge type and provide you energy usage. " +
                        "Ask me to start charging or to switch to solar. " +
                        "You can also ask me for an energy summary.")
                .withSimpleCard(Brand.NAME, "I can change your charge type and provide you energy usage. " +
                        "Ask me to start charging or to switch to solar. You can also ask me for an energy summary.")
                .withShouldEndSession(false)
                .build();
    }
}