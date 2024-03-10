package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myenergi.EvStatusSummary;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.handlers.responses.ZappiEvConnectionStatusCardResponse;
import com.amcglynn.myzappi.handlers.responses.ZappiEvConnectionStatusVoiceResponse;

import java.util.Locale;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class GetPlugStatusHandler implements RequestHandler {

    private final MyEnergiService.Builder zappiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;

    public GetPlugStatusHandler(MyEnergiService.Builder zappiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetPlugStatus"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var zappiService = zappiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput)).getZappiServiceOrThrow();
        var summary = new EvStatusSummary(zappiService.getStatusSummary().get(0));
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        return handlerInput.getResponseBuilder()
                .withSpeech(new ZappiEvConnectionStatusVoiceResponse(locale, summary).toString())
                .withSimpleCard(Brand.NAME,
                        new ZappiEvConnectionStatusCardResponse(locale, summary).toString())
                .withShouldEndSession(false)
                .build();
    }
}
