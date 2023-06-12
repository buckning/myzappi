package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.core.Brand;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class QuitHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("AMAZON.StopIntent")) ||
                handlerInput.matches(intentName("AMAZON.NoIntent")) ||
                handlerInput.matches(intentName("AMAZON.CancelIntent")) ||
                handlerInput.matches(intentName("AMAZON.NavigateHomeIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withShouldEndSession(true)
                .withSpeech("Thank you for using " + Brand.NAME)
                .withSimpleCard(Brand.NAME, "Thank you for using " + Brand.NAME + ".")
                .build();
    }
}
