package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.MyEnergiService;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class SetLibbiChargeFromGridDisabledHandler implements RequestHandler {
    private final MyEnergiService.Builder myEnergiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;

    public SetLibbiChargeFromGridDisabledHandler(MyEnergiService.Builder myEnergiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.myEnergiServiceBuilder = myEnergiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("SetLibbiChargeFromGridDisabled"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var userIdResolver = userIdResolverFactory.newUserIdResolver(handlerInput);
        var libbiService = myEnergiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput)).getLibbiServiceOrThrow();
        var userId = UserId.from(userIdResolver.getUserId());

        var creds = libbiService.validateMyEnergiAccountIsConfigured(userId);

        libbiService.setChargeFromGrid(creds, false);

        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "libbi-charge-from-grid-disabling"))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "libbi-charge-from-grid-disabling"))
                .withShouldEndSession(false)
                .build();
    }
}
