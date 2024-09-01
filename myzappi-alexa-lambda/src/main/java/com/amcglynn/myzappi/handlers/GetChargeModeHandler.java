package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class GetChargeModeHandler implements RequestHandler {

    private final MyEnergiService.Builder zappiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;

    public GetChargeModeHandler(MyEnergiService.Builder zappiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetChargeMode"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput));
        var summary = zappiService.getZappiServiceOrThrow().getStatusSummary().get(0);

        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "charge-mode", Map.of("chargeMode", summary.getChargeMode().getDisplayName())))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "charge-mode", Map.of("chargeMode", summary.getChargeMode().getDisplayName())))
                .withShouldEndSession(false)
                .build();
    }
}
