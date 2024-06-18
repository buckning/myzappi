package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.MyEnergiService;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class GetLibbiStateOfChargeHandler implements RequestHandler {
    private final MyEnergiService.Builder myEnergiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;

    public GetLibbiStateOfChargeHandler(MyEnergiService.Builder myEnergiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.myEnergiServiceBuilder = myEnergiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetLibbiStateOfCharge"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var userId = userIdResolverFactory.newUserIdResolver(handlerInput);
        var libbiService = myEnergiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput)).getLibbiServiceOrThrow();
        var status = libbiService.getStatus(UserId.from(userId.getUserId()));
        var stateOfCharge = status.getStateOfChargePercentage();

        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "libbi-state-of-charge", Map.of("chargePercentage", Integer.toString(stateOfCharge))))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "libbi-state-of-charge", Map.of("chargePercentage", Integer.toString(stateOfCharge))))
                .withShouldEndSession(false)
                .build();
    }
}
