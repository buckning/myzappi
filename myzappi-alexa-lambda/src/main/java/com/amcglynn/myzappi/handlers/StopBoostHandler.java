package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class StopBoostHandler implements RequestHandler {

    private final ZappiService.Builder zappiServiceBuilder;

    public StopBoostHandler(ZappiService.Builder zappiServiceBuilder) {
        this.zappiServiceBuilder = zappiServiceBuilder;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("StopBoostMode"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(handlerInput.getRequestEnvelope().getSession().getUser().getUserId());

        zappiService.stopBoost();
        return buildResponse(handlerInput);
    }

    private Optional<Response> buildResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech("Stopping boost mode now.")
                .withSimpleCard(Brand.NAME, "Stopping boost mode now.")
                .build();
    }
}
