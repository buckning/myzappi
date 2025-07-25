package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.directive.Header;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.model.services.directive.SpeakDirective;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.handlers.responses.ZappiStatusSummaryCardResponse;
import com.amcglynn.myzappi.handlers.responses.ZappiStatusSummaryVoiceResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.RequestAttributes.waitForHistory;
import static com.amcglynn.myzappi.RequestAttributes.waitForZappiStatusSummary;

@Slf4j
public class StatusSummaryHandler implements RequestHandler {
    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("StatusSummary"));
    }

    @SneakyThrows
    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        handlerInput.getServiceClientFactory().getDirectiveService()
                .enqueue(SendDirectiveRequest.builder()
                        .withDirective(SpeakDirective.builder().withSpeech("Sure").build())
                        .withHeader(Header.builder().withRequestId(handlerInput.getRequestEnvelope().getRequest().getRequestId()).build())
                        .build());
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());

        var summary = waitForZappiStatusSummary(handlerInput);

        try {
            waitForHistory(handlerInput);
        } catch (ExecutionException | InterruptedException e) {
            log.warn("Failed to get history", e);
        }

        return handlerInput.getResponseBuilder()
                .withSpeech(new ZappiStatusSummaryVoiceResponse(locale, summary).toString())
                .withSimpleCard(Brand.NAME, new ZappiStatusSummaryCardResponse(locale, summary).toString())
                .withShouldEndSession(false)
                .build();
    }
}
