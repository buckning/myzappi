package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.handlers.responses.ZappiDaySummaryCardResponse;
import com.amcglynn.myzappi.handlers.responses.ZappiDaySummaryVoiceResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class GetEnergyUsageGraphHandler implements RequestHandler {

    private final MyEnergiService.Builder zappyServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;
    private final UserZoneResolver userZoneResolver;

    public GetEnergyUsageGraphHandler(MyEnergiService.Builder zappyServiceBuilder, UserIdResolverFactory userIdResolverFactory,
                                      UserZoneResolver userZoneResolver) {
        this.zappyServiceBuilder = zappyServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
        this.userZoneResolver = userZoneResolver;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetEnergyUsage"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        // expected date format is 2023-05-06
        var date = parseDate(handlerInput);
        if (date.isEmpty() || date.get().length() != 10) {
            return getInvalidInputResponse(handlerInput);
        }
        var userTimeZone = userZoneResolver.getZoneId(handlerInput);
        var localDate = LocalDate.parse(date.get(), DateTimeFormatter.ISO_DATE);

        if (isInvalid(localDate, userTimeZone)) {
            return getInvalidRequestedDateResponse(handlerInput);
        }

        var zappiService = zappyServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput)).getZappiServiceOrThrow();
        var history = zappiService.getEnergyUsage(localDate, userTimeZone);

        return handlerInput.getResponseBuilder()
                .withSpeech(new ZappiDaySummaryVoiceResponse(locale, history).toString())
                .withSimpleCard(Brand.NAME,
                        new ZappiDaySummaryCardResponse(locale, history).toString())
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> getInvalidRequestedDateResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "invalid-future-date"))
                .withSimpleCard(Brand.NAME,
                        cardResponse(handlerInput, "invalid-future-date"))
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> getInvalidInputResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "request-specific-date"))
                .withSimpleCard(Brand.NAME,
                        cardResponse(handlerInput, "request-specific-date"))
                .withShouldEndSession(false)
                .build();
    }

    /**
     * Invalid if date is in the future. Requesting for today is accepted but not future dates.
     * @param date today or historical date
     * @param userTimeZone time-zone of the user
     * @return true if invalid
     */
    private boolean isInvalid(LocalDate date, ZoneId userTimeZone) {
        return date.isAfter(LocalDate.now(userTimeZone));
    }

    private Optional<String> parseDate(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue("date");
    }
}
