package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;

import java.time.Duration;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class BoostEddiHandler implements RequestHandler {

    private final ZappiService.Builder zappiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;

    public BoostEddiHandler(ZappiService.Builder zappiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("BoostEddi"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var duration = parseDurationSlot(handlerInput);

        if (duration.toMinutes() < 1 || duration.toMinutes() > 99) {
            return handlerInput.getResponseBuilder()
                    .withSpeech(voiceResponse(handlerInput, "invalid-eddi-boost-duration"))
                    .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "invalid-eddi-boost-duration"))
                    .withShouldEndSession(false)
                    .build();
        }

        var zappiService = zappiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput));

        zappiService.boostEddi(duration);

        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "boosting-eddi"))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "boosting-eddi"))
                .withShouldEndSession(false)
                .build();
    }

    private Optional<String> parseSlot(HandlerInput handlerInput, String slotName) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue(slotName);
    }

    private Duration parseDurationSlot(HandlerInput handlerInput) {
        return parseSlot(handlerInput, "Duration").map(Duration::parse).get();
    }
}
