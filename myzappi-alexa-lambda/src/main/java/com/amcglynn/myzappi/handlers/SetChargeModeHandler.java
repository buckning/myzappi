package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amazon.ask.response.ResponseBuilder;
import com.amcglynn.myenergi.EvStatusSummary;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.mappers.AlexaZappiChargeModeMapper;
import com.amcglynn.myzappi.service.ControlPanelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;
import static com.amcglynn.myzappi.RequestAttributes.waitForZappiStatusSummary;

@Slf4j
public class SetChargeModeHandler implements RequestHandler {

    private final AlexaZappiChargeModeMapper mapper;

    public SetChargeModeHandler() {
        mapper = new AlexaZappiChargeModeMapper();
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("SetChargeMode"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = getZappiServiceOrThrow(handlerInput);

        var request = handlerInput.getRequestEnvelope().getRequest();
        var intentRequest = (IntentRequest) request;
        var slots = intentRequest.getIntent().getSlots();
        var chargeModeSlot = slots.get("ChargeMode");

        var mappedChargeMode = mapper.getZappiChargeMode(chargeModeSlot.getValue().toLowerCase());

        ResponseBuilder responseBuilder = handlerInput.getResponseBuilder();
        if (mappedChargeMode.isEmpty()) {
            // it should not be possible to get to this block since Alexa should only allow requests with valid values in the slot
            return responseBuilder
                    .withSpeech(voiceResponse(handlerInput, "unrecognised-charge-mode"))
                    .withSimpleCard(Brand.NAME, "Sorry, I don't recognise that charge mode.")
                    .withShouldEndSession(false)
                    .build();
        }
        var newChargeMode = mappedChargeMode.get();

        zappiService.setChargeMode(newChargeMode);

        var voiceResponse = voiceResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", newChargeMode.getDisplayName()));
        var cardResponse = cardResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", newChargeMode.getDisplayName()));

        try {
            var zappiStatus = waitForZappiStatusSummary(handlerInput);
            var currentChargeMode = zappiStatus.getChargeMode();

            log.info("Current charge mode: {}, New charge mode: {}, connection status = {}", currentChargeMode, newChargeMode, new EvStatusSummary(zappiStatus));
            if (chargeModeEscalated(currentChargeMode, newChargeMode) && !new EvStatusSummary(zappiStatus).isConnected()) {
                voiceResponse += " " + voiceResponse(handlerInput, "connect-ev");
                cardResponse += "\n" + cardResponse(handlerInput, "connect-ev");
            }
            addControlPanel(handlerInput, responseBuilder, newChargeMode);
        } catch (ExecutionException | InterruptedException exception) {
            // it's not important if we can't get the Zappi status, we can still change the charge mode to what the user requested
            log.info("Failed to get Zappi status when changing the charge mode, ignoring...", exception);
        }


        return responseBuilder
                .withShouldEndSession(false)
                .withSpeech(voiceResponse)
                .withSimpleCard(Brand.NAME, cardResponse)
                .build();
    }

    private void addControlPanel(HandlerInput handlerInput, ResponseBuilder responseBuilder, ZappiChargeMode newChargeMode) {
        if (RequestHelper.forHandlerInput(handlerInput)
                .getSupportedInterfaces()
                .getAlexaPresentationAPL() != null) {
            responseBuilder
                    .addDirective(new ControlPanelBuilder().buildControlPanel(handlerInput, newChargeMode));
        }
    }

    /**
     * Check if the new charge mode is escalated from the existing charge mode. Escalated meaning the new charge mode
     * pushes a greater charge rate to the car from the previous charge mode. Stop -> Eco+ -> Eco -> Fast -> Boost
     * If the user is escalating the charge mode, they would likely want to know if their car is not plugged in.
     * @param existingChargeMode the current charge mode
     * @param newChargeMode the new charge mode requested by the user
     * @return if the new charge mode is escalated from the existing charge mode
     */
    private boolean chargeModeEscalated(ZappiChargeMode existingChargeMode, ZappiChargeMode newChargeMode) {
        return newChargeMode.ordinal() < existingChargeMode.ordinal();
    }
}
