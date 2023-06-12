package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.EvStatusSummary;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.handlers.responses.ZappiEvConnectionStatusCardResponse;
import com.amcglynn.myzappi.handlers.responses.ZappiEvConnectionStatusVoiceResponse;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class GetPlugStatusHandler implements RequestHandler {

    private final ZappiService.Builder zappiServiceBuilder;

    public GetPlugStatusHandler(ZappiService.Builder zappiServiceBuilder) {
        this.zappiServiceBuilder = zappiServiceBuilder;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetPlugStatus"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(handlerInput.getRequestEnvelope().getSession().getUser().getUserId());
        var summary = new EvStatusSummary(zappiService.getStatusSummary().get(0));
        return handlerInput.getResponseBuilder()
                .withSpeech(new ZappiEvConnectionStatusVoiceResponse(summary).toString())
                .withSimpleCard(Brand.NAME,
                        new ZappiEvConnectionStatusCardResponse(summary).toString())
                .build();
    }
}
