package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.ZappiService;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class StartBoostHandler implements RequestHandler {

    private final ZappiService.Builder zappiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;

    public StartBoostHandler(ZappiService.Builder zappiServiceBuilder, UserIdResolverFactory userIdResolverFactory) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("StartBoostMode"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var duration = parseDurationSlot(handlerInput);
        var time = parseSlot(handlerInput, "Time");
        var kilowattHours = parseKiloWattHourSlot(handlerInput);

        var zappiService = zappiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput));

        if (duration.isPresent()) {
            return buildResponse(handlerInput, zappiService.startSmartBoost(duration.get()));
        }
        if (time.isPresent()) {
            return buildResponse(handlerInput, zappiService.startSmartBoost(LocalTime.parse(time.get())));
        }
        if (kilowattHours.isPresent()) {
            zappiService.startBoost(kilowattHours.get());
            return buildResponse(handlerInput, kilowattHours.get());
        }

        return buildNotFoundResponse(handlerInput);
    }

    private Optional<String> parseSlot(HandlerInput handlerInput, String slotName) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue(slotName);
    }

    private Optional<Duration> parseDurationSlot(HandlerInput handlerInput) {
        return parseSlot(handlerInput, "Duration").map(Duration::parse);
    }

    private Optional<KiloWattHour> parseKiloWattHourSlot(HandlerInput handlerInput) {
        return parseSlot(handlerInput, "KiloWattHours").map(Double::parseDouble).map(KiloWattHour::new);
    }

    private Optional<Response> buildResponse(HandlerInput handlerInput, LocalTime endTime) {
        return handlerInput.getResponseBuilder()
                .withSpeech("Boosting until " + endTime.format(DateTimeFormatter.ofPattern("h:mm a")))
                .withSimpleCard(Brand.NAME, "Boosting until "
                        + endTime.format(DateTimeFormatter.ofPattern("h:mm a")) + ".")
                .build();
    }

    private Optional<Response> buildResponse(HandlerInput handlerInput, KiloWattHour kilowattHours) {
        return handlerInput.getResponseBuilder()
                .withSpeech("Charging " + kilowattHours + " kilowatt hours")
                .withSimpleCard("My Zappi", "Charging " + kilowattHours + " kilowatt hours")
                .build();
    }

    private Optional<Response> buildNotFoundResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech("Sorry, I didn't understand that")
                .withSimpleCard(Brand.NAME, "Sorry, I didn't understand that")
                .build();
    }
}
