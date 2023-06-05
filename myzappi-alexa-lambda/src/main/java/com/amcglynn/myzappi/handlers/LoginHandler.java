package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.Brand;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class LoginHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("RegisterCredentials"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var myZappiCode = "usl29d";
        var formattedCode = String.join(". ", myZappiCode.split("(?!^)"));
        var voiceResponse = "Thank you, your {brandName} code is " + formattedCode + ". Please use this on the {brandName} " +
                "website when configuring your API key";
        var cardResponse = "Thank you, your {brandName} code is " + myZappiCode + ". Please use this on the {brandName} " +
                "website when configuring your API key.";
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse.replace("{brandName}", Brand.NAME))
                .withSimpleCard(Brand.NAME, cardResponse.replace("{brandName}", Brand.NAME))
                .build();
    }
}
