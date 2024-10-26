package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.handlers.responses.GetChargeRateCardResponse;
import com.amcglynn.myzappi.handlers.responses.GetChargeRateVoiceResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;

@Slf4j
public class GetChargeRateHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetChargeRate"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());

        var summary = getZappiServiceOrThrow(handlerInput).getStatusSummary().get(0);

        return handlerInput.getResponseBuilder()
                .withSpeech(new GetChargeRateVoiceResponse(locale, summary).toString())
                .withSimpleCard(Brand.NAME, new GetChargeRateCardResponse(locale, summary).toString())
                .withShouldEndSession(false)
                .build();
    }
}
