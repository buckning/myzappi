package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.handlers.responses.CardResponse;
import com.amcglynn.myzappi.handlers.responses.VoiceResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.amcglynn.myzappi.handlers.responses.VoiceResponse.NOT_FOUND;

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
                .withSpeech(getVoiceResponse(throwable))
                .withSimpleCard(Brand.NAME, getCardResponse(throwable))
                .build();
    }

    private String getVoiceResponse(Throwable throwable) {
        var response = VoiceResponse.get(throwable.getClass());
        if (NOT_FOUND.equals(response)) {
            response = VoiceResponse.get(Exception.class);
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
