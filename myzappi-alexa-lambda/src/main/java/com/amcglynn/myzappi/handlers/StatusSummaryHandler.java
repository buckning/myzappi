package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.directive.Header;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.model.services.directive.SpeakDirective;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.handlers.responses.ZappiStatusSummaryCardResponse;
import com.amcglynn.myzappi.handlers.responses.ZappiStatusSummaryVoiceResponse;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class StatusSummaryHandler implements RequestHandler {

    private final ZappiService.Builder zappiServiceBuilder;

    public StatusSummaryHandler(ZappiService.Builder zappiServiceBuilder) {
        this.zappiServiceBuilder = zappiServiceBuilder;
    }


    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("StatusSummary"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        handlerInput.getServiceClientFactory().getDirectiveService()
                .enqueue(SendDirectiveRequest.builder()
                        .withDirective(SpeakDirective.builder().withSpeech("Sure").build())
                        .withHeader(Header.builder().withRequestId(handlerInput.getRequestEnvelope().getRequest().getRequestId()).build())
                        .build());

        var zappiService = zappiServiceBuilder.build(handlerInput.getRequestEnvelope().getSession().getUser().getUserId());
        var summary = zappiService.getStatusSummary().get(0);

        return handlerInput.getResponseBuilder()
                .withSpeech(new ZappiStatusSummaryVoiceResponse(summary).toString())
                .withSimpleCard(Brand.NAME, new ZappiStatusSummaryCardResponse(summary).toString())
                .build();
    }
}
