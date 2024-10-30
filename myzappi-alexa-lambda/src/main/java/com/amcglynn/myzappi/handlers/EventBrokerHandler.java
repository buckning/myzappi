package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.UserEvent;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.Brand;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.requestType;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;

public class EventBrokerHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput input) {
        // Check if the request is an APL UserEvent request
        return input.matches(requestType(UserEvent.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var userEvent = (UserEvent) handlerInput.getRequestEnvelope().getRequest();

        // Extract arguments from the UserEvent request
        if (userEvent.getArguments() != null && !userEvent.getArguments().isEmpty()) {
            var command = userEvent.getArguments().get(0).toString();

            if (command.equals("setChargeMode")) {
                return handleSetChargeMode(handlerInput, userEvent);
            }
            // Perform actions based on event argument
            System.out.println("Received APL UserEvent with argument: " + command);

            // Add any logic based on the user event, such as handling button clicks or other interactions
        }

        // Send a response if needed (can be empty or confirm interaction)
        return handlerInput.getResponseBuilder()
                .withSpeech("Received your selection!")
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> handleSetChargeMode(HandlerInput handlerInput, UserEvent userEvent) {
        var chargeMode = userEvent.getArguments().get(1).toString();

        var newChargeMode = ZappiChargeMode.valueOf(chargeMode);
        getZappiServiceOrThrow(handlerInput).setChargeMode(newChargeMode);
        var voiceResponse = voiceResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", newChargeMode.getDisplayName()));
        var cardResponse = cardResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", newChargeMode.getDisplayName()));
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse)
                .withSimpleCard(Brand.NAME, cardResponse)
                .withShouldEndSession(false)
                .build();
    }
}