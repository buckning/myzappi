package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.LaunchRequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.RenderDocumentDirective;
import com.amazon.ask.request.RequestHelper;
import com.amazon.ask.response.ResponseBuilder;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.handlers.responses.ZappiStatusSummaryCardResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.requestType;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;

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
                        .addDirective(buildControlPanel(handlerInput))
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

    private RenderDocumentDirective buildControlPanel(HandlerInput handlerInput) {
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        var summary = getZappiServiceOrThrow(handlerInput).getStatusSummary();
        var content = new ZappiStatusSummaryCardResponse(locale, summary.get(0)).toString().replace("\n", "<br>");

        return RenderDocumentDirective.builder()
                .withToken("zappidaysummaryToken")
                .withDocument(buildDocument(content))
                .build();
    }

    @SneakyThrows
    private Map<String, Object> buildDocument(String summaryContent) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> documentMapType = new TypeReference<>() {
        };

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("apl/zappi-control-panel.json");

        var contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        contents = contents.replace("${payload.energySummary}", summaryContent);

        return mapper.readValue(contents, documentMapType);
    }

    private boolean hasDisplayInterface(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSupportedInterfaces()
                .getAlexaPresentationAPL() != null;
    }
}
