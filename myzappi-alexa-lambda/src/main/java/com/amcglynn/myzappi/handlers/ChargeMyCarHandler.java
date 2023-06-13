package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class ChargeMyCarHandler implements RequestHandler {

    private final ZappiService.Builder zappiServiceBuilder;

    public ChargeMyCarHandler(ZappiService.Builder zappiServiceBuilder) {
        this.zappiServiceBuilder = zappiServiceBuilder;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("ChargeMyCar"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(handlerInput.getRequestEnvelope().getSession().getUser().getUserId());
        zappiService.setChargeMode(ZappiChargeMode.FAST);
        return handlerInput.getResponseBuilder()
                .withSpeech("Changed charging mode to " + ZappiChargeMode.FAST.getDisplayName() + ". This may take a few minutes.")
                .withSimpleCard(Brand.NAME, "Changed charging mode to "
                        + ZappiChargeMode.FAST.getDisplayName() + ". This may take a few minutes.")
                .build();
    }
}
