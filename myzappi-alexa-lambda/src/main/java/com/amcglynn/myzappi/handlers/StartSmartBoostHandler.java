package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myenergi.units.KiloWattHour;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;

@Slf4j
public class StartSmartBoostHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("StartSmartBoost"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var kilowattHours = parseKiloWattHourSlot(handlerInput);
        var finishChargingAt = parseSlot(handlerInput, "Time");

        if (kilowattHours.isEmpty() || finishChargingAt.isEmpty()) {
            return handlerInput.getResponseBuilder()
                    .withSpeech(voiceResponse(handlerInput, "start-smart-boost-params-missing"))
                    .withSimpleCard("My Zappi", cardResponse(handlerInput, "start-smart-boost-params-missing"))
                    .withShouldEndSession(false)
                    .build();
        }

        var zappiService = getZappiServiceOrThrow(handlerInput);

        // alexa ensures kilowatt hours and Time slots are populated
        zappiService.startSmartBoost(kilowattHours.get(), LocalTime.parse(finishChargingAt.get()));
        return buildResponse(handlerInput, kilowattHours.get(), LocalTime.parse(finishChargingAt.get()));
    }

    private Optional<String> parseSlot(HandlerInput handlerInput, String slotName) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue(slotName);
    }

    private Optional<KiloWattHour> parseKiloWattHourSlot(HandlerInput handlerInput) {
        return parseSlot(handlerInput, "KiloWattHours").map(Double::parseDouble).map(KiloWattHour::new);
    }

    private Optional<Response> buildResponse(HandlerInput handlerInput, KiloWattHour kilowattHours, LocalTime finishChargingAt) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "start-smart-boost", Map.of("kWh", kilowattHours.toString(),
                        "time", finishChargingAt.toString())))
                .withSimpleCard("My Zappi", cardResponse(handlerInput, "start-smart-boost",
                        Map.of("kWh", kilowattHours.toString(), "time", finishChargingAt.toString())))
                .withShouldEndSession(false)
                .build();
    }
}
