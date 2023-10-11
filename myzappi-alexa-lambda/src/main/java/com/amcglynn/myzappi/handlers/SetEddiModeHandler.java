package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.mappers.AlexaEddiModeMapper;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class SetEddiModeHandler implements RequestHandler {

    private final ZappiService.Builder zappiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;
    private final AlexaEddiModeMapper mapper;

    public SetEddiModeHandler(ZappiService.Builder zappiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        this.mapper = new AlexaEddiModeMapper();
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("SetEddiMode"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput));

        var request = handlerInput.getRequestEnvelope().getRequest();
        var intentRequest = (IntentRequest) request;
        var slots = intentRequest.getIntent().getSlots();
        var chargeModeSlot = slots.get("eddiMode");

        var eddiMode = mapper.getEddiMode(chargeModeSlot.getValue().toLowerCase());

        if (eddiMode.isEmpty()) {
            // it should not be possible to get to this block since Alexa should only allow requests with valid values in the slot
            return handlerInput.getResponseBuilder()
                    .withSpeech(voiceResponse(handlerInput, "unrecognised-mode"))
                    .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "unrecognised-mode"))
                    .withShouldEndSession(false)
                    .build();
        }

        zappiService.setEddiMode(eddiMode.get());
        return handlerInput.getResponseBuilder()
                .withShouldEndSession(false)
                .withSpeech(voiceResponse(handlerInput, "change-eddi-mode", Map.of("eddiMode", eddiMode.get().name())))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "change-eddi-mode", Map.of("eddiMode", eddiMode.get().name())))
                .build();
    }
}
