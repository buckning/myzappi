package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.UserEvent;
import com.amazon.ask.request.RequestHelper;
import com.amazon.ask.response.ResponseBuilder;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.service.ControlPanelBuilder;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.requestType;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;

@AllArgsConstructor
public class EventBrokerHandler implements RequestHandler {

    private ScheduleService scheduleService;
    private UserIdResolverFactory userIdResolverFactory;

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(requestType(UserEvent.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var userEvent = (UserEvent) handlerInput.getRequestEnvelope().getRequest();

        if (userEvent.getArguments() != null && !userEvent.getArguments().isEmpty()) {
            var command = userEvent.getArguments().get(0).toString();

            if (command.equals("setChargeMode")) {
                return handleSetChargeMode(handlerInput, userEvent);
            }
            if (command.equals("deleteSchedule")) {
                return handleDeleteSchedule(handlerInput, userEvent);
            }
        }

        return handlerInput.getResponseBuilder()
                .withSpeech("Oops, Andrew didn't write code to handle this!")
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> handleSetChargeMode(HandlerInput handlerInput, UserEvent userEvent) {
        var chargeMode = userEvent.getArguments().get(1).toString();

        var newChargeMode = ZappiChargeMode.valueOf(chargeMode);
        getZappiServiceOrThrow(handlerInput).setChargeMode(newChargeMode);
        var voiceResponse = voiceResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", newChargeMode.getDisplayName()));
        var cardResponse = cardResponse(handlerInput, "change-charge-mode", Map.of("zappiChargeMode", newChargeMode.getDisplayName()));
        var responseBuilder = handlerInput.getResponseBuilder();
        addControlPanel(handlerInput, responseBuilder, newChargeMode);
        return responseBuilder
                .withSpeech(voiceResponse)
                .withSimpleCard(Brand.NAME, cardResponse)
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> handleDeleteSchedule(HandlerInput handlerInput, UserEvent userEvent) {
        var userIdResolver = userIdResolverFactory.newUserIdResolver(handlerInput);
        var scheduleId = userEvent.getArguments().get(1).toString();

        scheduleService.deleteSchedule(UserId.from(userIdResolver.getUserId()), scheduleId);

        var responseBuilder = handlerInput.getResponseBuilder();
        return responseBuilder
                .withSpeech("Schedule deleted")
                .withSimpleCard(Brand.NAME, "Schedule deleted")
                .withShouldEndSession(false)
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
}