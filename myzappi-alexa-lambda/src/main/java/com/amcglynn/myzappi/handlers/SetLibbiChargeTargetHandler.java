package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myenergi.LibbiMode;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.MyEnergiService;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class SetLibbiChargeTargetHandler implements RequestHandler {
    private final MyEnergiService.Builder myEnergiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;

    public SetLibbiChargeTargetHandler(MyEnergiService.Builder myEnergiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.myEnergiServiceBuilder = myEnergiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("SetLibbiChargeTarget"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var libbiService = myEnergiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput)).getLibbiServiceOrThrow();

        var userIdResolver = userIdResolverFactory.newUserIdResolver(handlerInput);
        var userId = UserId.from(userIdResolver.getUserId());

        var percentChargeTarget = parseChargePercentSlot(handlerInput);
        if (percentChargeTarget.isPresent()) {
            libbiService.setChargeTarget(userId, percentChargeTarget.get());
            return handlerInput.getResponseBuilder()
                    .withSpeech(voiceResponse(handlerInput, "libbi-setting-charge-target-percent",
                            Map.of("chargeTargetPercent", Integer.toString(percentChargeTarget.get()))))
                    .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "libbi-setting-charge-target-percent",
                            Map.of("chargeTargetPercent", Integer.toString(percentChargeTarget.get()))))
                    .withShouldEndSession(false)
                    .build();
        }


        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "libbi-set-charge-target-error-charge-target-percent-missing"))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "libbi-set-charge-target-error-charge-target-percent-missing"))
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Integer> parseChargePercentSlot(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue("ChargeTargetPercent").map(Integer::parseInt);
    }
}
