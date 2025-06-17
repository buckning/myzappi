package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.MyEnergiService;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class SetEddiModeToNormalHandler implements RequestHandler {

    private final MyEnergiService.Builder zappiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;

    public SetEddiModeToNormalHandler(MyEnergiService.Builder zappiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("SetEddiModeToNormal"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput)).getEddiServiceOrThrow();
        var eddiMode = EddiMode.NORMAL;
        zappiService.setEddiMode(eddiMode);
        return handlerInput.getResponseBuilder()
                .withShouldEndSession(false)
                .withSpeech(voiceResponse(handlerInput, "change-eddi-mode", Map.of("eddiMode", eddiMode.name().toLowerCase())))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "change-eddi-mode", Map.of("eddiMode", eddiMode.name().toLowerCase())))
                .build();
    }
}
