package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.LaunchRequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.service.ControlPanelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.requestType;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class LaunchHandler implements LaunchRequestHandler {

    private final Properties properties;

    public LaunchHandler() {
        this.properties = new Properties();
    }

    public LaunchHandler(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput, LaunchRequest launchRequest) {
        return handlerInput.matches(requestType(LaunchRequest.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput, LaunchRequest launchRequest) {
        var responseBuilder = handlerInput.getResponseBuilder();
        if (handlerInput.getRequestEnvelope().getSession()
                .getApplication().getApplicationId().equals(properties.getEddiSkillId())) {
            return responseBuilder
                    .withSpeech(voiceResponse(handlerInput, "eddi-help"))
                    .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "eddi-help"))
                    .withShouldEndSession(false)
                    .build();
        }

        if (handlerInput.getRequestEnvelope().getSession()
                .getApplication().getApplicationId().equals(properties.getLibbiSkillId())) {
            return responseBuilder
                    .withSpeech(voiceResponse(handlerInput, "libbi-help"))
                    .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "libbi-help"))
                    .withShouldEndSession(false)
                    .build();
        }

        if (hasDisplayInterface(handlerInput)) {
            try {
                return responseBuilder
                        .withSpeech("OK")
                        .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "help"))
                        .addDirective(new ControlPanelBuilder().buildControlPanel(handlerInput))
                        .withShouldEndSession(false)
                        .build();
            } catch (MissingDeviceException e) {
                // not going to do anything if there is no zappi configured
            }
        }
        return responseBuilder
                .withSpeech(voiceResponse(handlerInput, "help"))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "help"))
                .withShouldEndSession(false)
                .build();
    }

    private boolean hasDisplayInterface(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSupportedInterfaces()
                .getAlexaPresentationAPL() != null;
    }
}
