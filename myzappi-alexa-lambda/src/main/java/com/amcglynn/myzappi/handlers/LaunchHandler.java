package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.LaunchRequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.config.Properties;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.requestType;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class LaunchHandler implements LaunchRequestHandler {

    private Properties properties;

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
        if (handlerInput.getRequestEnvelope().getSession()
                .getApplication().getApplicationId().equals(properties.getEddiSkillId())) {
            return handlerInput.getResponseBuilder()
                    .withSpeech(voiceResponse(handlerInput, "eddi-help"))
                    .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "eddi-help"))
                    .withShouldEndSession(false)
                    .build();
        }

        if (handlerInput.getRequestEnvelope().getSession()
                .getApplication().getApplicationId().equals(properties.getLibbiSkillId())) {
            return handlerInput.getResponseBuilder()
                    .withSpeech(voiceResponse(handlerInput, "libbi-help"))
                    .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "libbi-help"))
                    .withShouldEndSession(false)
                    .build();
        }

        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "help"))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "help"))
                .withShouldEndSession(false)
                .build();
    }
}
