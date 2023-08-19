package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.LocalisedResponse;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.handlers.responses.CardResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class MyZappiExceptionHandler implements ExceptionHandler {
    @Override
    public boolean canHandle(HandlerInput handlerInput, Throwable throwable) {
        return true;
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput, Throwable throwable) {
        var userId = handlerInput.getRequestEnvelope().getSession().getUser().getUserId();
        log.error("Unexpected error not handled. userId = {}.", userId, throwable);

        return handlerInput.getResponseBuilder()
                .withSpeech(getVoiceResponse(handlerInput, throwable))
                .withSimpleCard(Brand.NAME, getCardResponse(throwable))
                .withShouldEndSession(false)
                .build();
    }

    private String getVoiceResponse(HandlerInput handlerInput, Throwable throwable) {
        String response;
        try {
            response = voiceResponse(handlerInput, "error." + throwable.getClass().getSimpleName());
        } catch (Exception e) {
            response = voiceResponse(handlerInput, "error.Exception");
        }
        return response;
    }

    private String getCardResponse(Throwable throwable) {
        var response = CardResponse.get(throwable.getClass());
        if (CardResponse.NOT_FOUND.equals(response)) {
            response = CardResponse.get(Exception.class);
        }
        return response;
    }
}
