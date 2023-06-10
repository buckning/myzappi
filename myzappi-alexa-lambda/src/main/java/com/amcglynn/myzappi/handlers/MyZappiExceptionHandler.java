package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.core.Brand;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

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
                .withSpeech("There was an unexpected error.")
                .withSimpleCard(Brand.NAME, "There was an unexpected error.")
                .build();
    }
}
