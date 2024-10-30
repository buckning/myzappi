package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.RenderDocumentDirective;
import com.amcglynn.myzappi.UserNotLinkedException;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.exception.UserNotLoggedInException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class MyZappiExceptionHandler implements ExceptionHandler {
    @Override
    public boolean canHandle(HandlerInput handlerInput, Throwable throwable) {
        return true;
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput, Throwable throwable) {
        log.error("Unexpected error not handled", throwable);

        var responseBuilder = handlerInput.getResponseBuilder()
                .withSpeech(getVoiceResponse(handlerInput, throwable))
                .withSimpleCard(Brand.NAME, getCardResponse(handlerInput, throwable))
                .withShouldEndSession(false);

        if (throwable instanceof UserNotLoggedInException e) {
            return buildUserMustRegisterResponse(handlerInput);
        }
        if (throwable instanceof UserNotLinkedException e) {
            responseBuilder = responseBuilder.withLinkAccountCard();
        }

        return responseBuilder.build();
    }

    private String getVoiceResponse(HandlerInput handlerInput, Throwable throwable) {
        String response;
        try {
            response = voiceResponse(handlerInput, "error." + throwable.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Error getting voice response", e);
            response = voiceResponse(handlerInput, "error.Exception");
        }
        return response;
    }

    private String getCardResponse(HandlerInput handlerInput, Throwable throwable) {
        String response;
        try {
            response = cardResponse(handlerInput, "error." + throwable.getClass().getSimpleName());
        } catch (Exception e) {
            response = voiceResponse(handlerInput, "error.Exception");
        }
        return response;
    }

    private Optional<Response> buildUserMustRegisterResponse(HandlerInput handlerInput) {
        var doc = RenderDocumentDirective.builder()
                .withToken("zappidaysummaryToken")
                .withDocument(buildRegisterAplDocument())
                .build();
        return handlerInput.getResponseBuilder()
                .addDirective(doc)
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "error.UserNotLoggedInException"))
                .withShouldEndSession(false)
                .build();
    }

    @SneakyThrows
    private Map<String, Object> buildRegisterAplDocument() {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> documentMapType = new TypeReference<>() {
        };

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("apl/register.json");

        var contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        return mapper.readValue(contents, documentMapType);
    }

}
