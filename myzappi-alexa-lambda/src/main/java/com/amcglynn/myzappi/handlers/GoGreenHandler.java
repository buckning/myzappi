package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.handlers.responses.CardResponse;
import com.amcglynn.myzappi.handlers.responses.VoiceResponse;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class GoGreenHandler implements RequestHandler {

    private final ZappiService.Builder zappiServiceBuilder;

    public GoGreenHandler(ZappiService.Builder zappiServiceBuilder) {
        this.zappiServiceBuilder = zappiServiceBuilder;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GoGreen"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(handlerInput.getRequestEnvelope().getSession().getUser().getUserId());
        var chargeMode = ZappiChargeMode.ECO_PLUS;
        zappiService.setChargeMode(chargeMode);
        return handlerInput.getResponseBuilder()
                .withSpeech(VoiceResponse.get(ZappiChargeMode.class).replace("{zappiChargeMode}", chargeMode.getDisplayName()))
                .withSimpleCard(Brand.NAME, CardResponse.get(ZappiChargeMode.class).replace("{zappiChargeMode}", chargeMode.getDisplayName()))
                .build();
    }
}
