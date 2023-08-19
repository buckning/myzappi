package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myzappi.TariffNotFoundException;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.TariffService;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.handlers.responses.ZappiEnergyCostCardResponse;
import com.amcglynn.myzappi.handlers.responses.ZappiEnergyCostVoiceResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class GetEnergyCostHandler implements RequestHandler {
    private final ZappiService.Builder zappyServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;
    private final UserZoneResolver userZoneResolver;
    private final TariffService tariffService;

    public GetEnergyCostHandler(ZappiService.Builder zappyServiceBuilder, UserIdResolverFactory userIdResolverFactory,
                                UserZoneResolver userZoneResolver, TariffService tariffService) {
        this.zappyServiceBuilder = zappyServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
        this.userZoneResolver = userZoneResolver;
        this.tariffService = tariffService;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetEnergyCost"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        // expected date format is 2023-05-06
        var userTimeZone = userZoneResolver.getZoneId(handlerInput);

        var date = parseSlot(handlerInput);
        if (date.isPresent() && date.get().length() != 10) {
            return getInvalidInputResponse(handlerInput);
        }

        var localDate = date.map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_DATE))
                .orElse(LocalDate.now(userTimeZone));

        if (isInvalid(localDate, userTimeZone)) {
            return getInvalidRequestedDateResponse(handlerInput);
        }

        var userIdResolver = userIdResolverFactory.newUserIdResolver(handlerInput);
        var userId = userIdResolver.getUserId();
        var dayTariff = tariffService.get(userId).orElseThrow(() -> new TariffNotFoundException(userId));

        var zappiService = zappyServiceBuilder.build(userIdResolver);

        // call getHistory instead of getHourlyHistory so that requests for "today" are always up to date
        var history = zappiService.getHistory(localDate, userTimeZone);

        // Tariffs are local time and history is UTC. This needs to be converted first so date and time zone need to be
        // handled in the cost calculation
        var cost = tariffService.calculateCost(dayTariff, history, localDate, userTimeZone);

        return handlerInput.getResponseBuilder()
                .withSpeech(new ZappiEnergyCostVoiceResponse(locale, cost).toString())
                .withSimpleCard(Brand.NAME,
                        new ZappiEnergyCostCardResponse(cost).toString())
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> getInvalidRequestedDateResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "invalid-future-date-cost"))
                .withSimpleCard(Brand.NAME,
                        "I cannot give you a cost for a time in the future.")
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> getInvalidInputResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "request-specific-date-cost"))
                .withSimpleCard(Brand.NAME,
                        "Please ask me for an energy cost for a specific day.")
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

    private Optional<String> parseSlot(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue("date");
    }
}
