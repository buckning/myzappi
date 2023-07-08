package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.handlers.responses.CardResponse;
import com.amcglynn.myzappi.handlers.responses.VoiceResponse;
import com.amcglynn.myzappi.mappers.AlexaZappiChargeModeMapper;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class SetChargeModeHandler implements RequestHandler {

    private final ZappiService.Builder zappiServiceBuilder;
    private final AlexaZappiChargeModeMapper mapper;
    private final UserIdResolverFactory userIdResolverFactory;

    public SetChargeModeHandler(ZappiService.Builder zappiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        mapper = new AlexaZappiChargeModeMapper();
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("SetChargeMode"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput));

        var request = handlerInput.getRequestEnvelope().getRequest();
        var intentRequest = (IntentRequest) request;
        var slots = intentRequest.getIntent().getSlots();
        var chargeModeSlot = slots.get("ChargeMode");

        var mappedChargeMode = mapper.getZappiChargeMode(chargeModeSlot.getValue().toLowerCase());

        if (mappedChargeMode.isEmpty()) {
            // it should not be possible to get to this block since Alexa should only allow requests with valid values in the slot
            return handlerInput.getResponseBuilder()
                    .withSpeech("Sorry, I don't recognise that charge mode.")
                    .withSimpleCard(Brand.NAME, "Sorry, I don't recognise that charge mode.")
                    .withShouldEndSession(false)
                    .build();
        }
        var chargeMode = mappedChargeMode.get();

        zappiService.setChargeMode(chargeMode);
        return handlerInput.getResponseBuilder()
                .withShouldEndSession(false)
                .withSpeech(VoiceResponse.get(ZappiChargeMode.class).replace("{zappiChargeMode}", chargeMode.getDisplayName()))
                .withSimpleCard(Brand.NAME, CardResponse.get(ZappiChargeMode.class).replace("{zappiChargeMode}", chargeMode.getDisplayName()))
                .build();
    }
}
