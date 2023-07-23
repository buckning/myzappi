package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.handlers.responses.CardResponse;
import com.amcglynn.myzappi.handlers.responses.VoiceResponse;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class UnlockZappiHandler implements RequestHandler {

    private final ZappiService.Builder zappiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;

    public UnlockZappiHandler(ZappiService.Builder zappiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("UnlockZappi"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput));
        zappiService.unlockZappi();
        return handlerInput.getResponseBuilder()
                .withSpeech(VoiceResponse.get(UnlockZappiHandler.class))
                .withSimpleCard(Brand.NAME, CardResponse.get(UnlockZappiHandler.class))
                .withShouldEndSession(false)
                .build();
    }
}
