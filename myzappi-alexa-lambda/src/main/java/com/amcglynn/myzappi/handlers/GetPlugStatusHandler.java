package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.EvStatusSummary;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.handlers.responses.ZappiEvConnectionStatusCardResponse;
import com.amcglynn.myzappi.handlers.responses.ZappiEvConnectionStatusVoiceResponse;
import lombok.SneakyThrows;

import java.util.Locale;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.RequestAttributes.waitForZappiStatusSummary;

public class GetPlugStatusHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetPlugStatus"));
    }

    @SneakyThrows
    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var summary = new EvStatusSummary(waitForZappiStatusSummary(handlerInput));
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        return handlerInput.getResponseBuilder()
                .withSpeech(new ZappiEvConnectionStatusVoiceResponse(locale, summary).toString())
                .withSimpleCard(Brand.NAME,
                        new ZappiEvConnectionStatusCardResponse(locale, summary).toString())
                .withShouldEndSession(false)
                .build();
    }
}
