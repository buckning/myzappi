package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.core.Brand;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class FallbackHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("AMAZON.FallbackIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech("Sorry, I don't know how to handle that. Please try again.")
                .withSimpleCard(Brand.NAME, "Sorry, I don't know how to handle that. Please try again.")
                .build();
    }
}