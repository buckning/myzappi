package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.handlers.responses.ZappiDaySummaryCardResponse;
import com.amcglynn.myzappi.handlers.responses.ZappiDaySummaryVoiceResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class GetEnergyUsageHandler implements RequestHandler {

    private final ZappiService.Builder zappyServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;
    private final UserZoneResolver userZoneResolver;

    public GetEnergyUsageHandler(ZappiService.Builder zappyServiceBuilder, UserIdResolverFactory userIdResolverFactory,
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
        var date = parseSlot(handlerInput, "date");
        if (date.isEmpty() || date.get().length() != 10) {
            return getInvalidInputResponse(handlerInput);
        }
        var userTimeZone = userZoneResolver.getZoneId(handlerInput);
        var localDate = LocalDate.parse(date.get(), DateTimeFormatter.ISO_DATE);

        if (isInvalid(localDate, userTimeZone)) {
            return getInvalidRequestedDateResponse(handlerInput);
        }

        var zappiService = zappyServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput));
        var history = zappiService.getEnergyUsage(localDate, userTimeZone);

        return handlerInput.getResponseBuilder()
                .withSpeech(new ZappiDaySummaryVoiceResponse(locale, history).toString())
                .withSimpleCard(Brand.NAME,
                        new ZappiDaySummaryCardResponse(history).toString())
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> getInvalidRequestedDateResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "invalid-future-date"))
                .withSimpleCard(Brand.NAME,
                        "I cannot give you usage data for a time in the future.")
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> getInvalidInputResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "request-specific-date"))
                .withSimpleCard(Brand.NAME,
                        "Please ask me for energy usage for a specific day.")
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

    private Optional<String> parseSlot(HandlerInput handlerInput, String slotName) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue(slotName);
    }
}
