package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.UserNotLinkedException;
import com.amcglynn.myzappi.core.Brand;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class MyZappiExceptionHandler implements ExceptionHandler {
    @Override
    public boolean canHandle(HandlerInput handlerInput, Throwable throwable) {
        return true;
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput, Throwable throwable) {
        log.error("Unexpected error not handled", throwable);

        var responseBuilder = handlerInput.getResponseBuilder()
                .withSpeech(getVoiceResponse(handlerInput, throwable))
                .withSimpleCard(Brand.NAME, getCardResponse(handlerInput, throwable))
                .withShouldEndSession(false);

        if (throwable instanceof UserNotLinkedException e) {
            responseBuilder = responseBuilder.withLinkAccountCard();
        }

        return responseBuilder.build();
    }

    private String getVoiceResponse(HandlerInput handlerInput, Throwable throwable) {
        String response;
        try {
            response = voiceResponse(handlerInput, "error." + throwable.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Error getting voice response", e);
            response = voiceResponse(handlerInput, "error.Exception");
        }
        return response;
    }

    private String getCardResponse(HandlerInput handlerInput, Throwable throwable) {
        String response;
        try {
            response = cardResponse(handlerInput, "error." + throwable.getClass().getSimpleName());
        } catch (Exception e) {
            response = voiceResponse(handlerInput, "error.Exception");
        }
        return response;
    }
}
