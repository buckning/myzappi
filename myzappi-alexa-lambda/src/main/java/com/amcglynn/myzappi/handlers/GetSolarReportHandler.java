package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.directive.Header;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.model.services.directive.SpeakDirective;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.handlers.responses.SolarReportCardResponse;
import com.amcglynn.myzappi.handlers.responses.SolarReportVoiceResponse;

import java.util.Locale;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;

public class GetSolarReportHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetSolarReport"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        handlerInput.getServiceClientFactory().getDirectiveService()
                .enqueue(SendDirectiveRequest.builder()
                        .withDirective(SpeakDirective.builder().withSpeech("Sure").build())
                        .withHeader(Header.builder().withRequestId(handlerInput.getRequestEnvelope().getRequest().getRequestId()).build())
                        .build());
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());

        var summary = getZappiServiceOrThrow(handlerInput).getStatusSummary().get(0);

        return handlerInput.getResponseBuilder()
                .withSpeech(new SolarReportVoiceResponse(locale, summary).toString())
                .withSimpleCard(Brand.NAME, new SolarReportCardResponse(locale, summary).toString())
                .withShouldEndSession(false)
                .build();
    }
}
