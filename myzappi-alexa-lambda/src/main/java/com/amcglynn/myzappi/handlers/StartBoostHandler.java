package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myenergi.units.KiloWattHour;

import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;
import static com.amcglynn.myzappi.RequestAttributes.getZoneId;

public class StartBoostHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("StartBoostMode"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var kilowattHours = parseKiloWattHourSlot(handlerInput);

        var zappiService = getZappiServiceOrThrow(handlerInput);

        var zoneId = getZoneId(handlerInput);
        zappiService.setLocalTimeSupplier(() -> LocalTime.now(zoneId));

        // alexa ensures kilowatt hours slot is populated
        zappiService.startBoost(kilowattHours.get());
        return buildResponse(handlerInput, kilowattHours.get());
    }

    private Optional<String> parseSlot(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue("KiloWattHours");
    }

    private Optional<KiloWattHour> parseKiloWattHourSlot(HandlerInput handlerInput) {
        return parseSlot(handlerInput).map(Double::parseDouble).map(KiloWattHour::new);
    }

    private Optional<Response> buildResponse(HandlerInput handlerInput, KiloWattHour kilowattHours) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "boosting-for-kwh", Map.of("kWh", kilowattHours.toString())))
                .withSimpleCard("My Zappi", cardResponse(handlerInput, "boosting-for-kwh", Map.of("kWh", kilowattHours.toString())))
                .withShouldEndSession(false)
                .build();
    }
}
