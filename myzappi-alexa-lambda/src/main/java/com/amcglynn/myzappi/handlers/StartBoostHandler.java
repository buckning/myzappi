package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.MyEnergiService;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

public class StartBoostHandler implements RequestHandler {

    private final MyEnergiService.Builder zappiServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;
    private final UserZoneResolver userZoneResolver;

    public StartBoostHandler(MyEnergiService.Builder zappiServiceBuilder, UserIdResolverFactory userIdResolverFactory,
                             UserZoneResolver userZoneResolver) {
        this.zappiServiceBuilder = zappiServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
        this.userZoneResolver = userZoneResolver;
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

        var zappiService = zappiServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput)).getZappiServiceOrThrow();
        // The boost API when duration is used needs to have the local time read from the device that made the request.
        // This is done by reading the base URL and access token from the request and making a request to Alexa API
        // to retrieve the timezone for the device. Once the timezone is retrieved, it is set here so that when an end
        // time needs to be calculated, it gets the current local time, adds the duration and then sends that end time
        // to the myenergi API

        var zoneId = userZoneResolver.getZoneId(handlerInput);
        zappiService.setLocalTimeSupplier(() -> LocalTime.now(zoneId));

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
                .withSpeech(voiceResponse(handlerInput, "boosting-until-time", Map.of("time",
                        endTime.format(DateTimeFormatter.ofPattern("h:mm a")))))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "boosting-until-time", Map.of("time",
                        endTime.format(DateTimeFormatter.ofPattern("h:mm a")))))
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> buildResponse(HandlerInput handlerInput, KiloWattHour kilowattHours) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "boosting-for-kwh", Map.of("kWh", kilowattHours.toString())))
                .withSimpleCard("My Zappi", cardResponse(handlerInput, "boosting-for-kwh", Map.of("kWh", kilowattHours.toString())))
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> buildNotFoundResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "didnt-understand"))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "didnt-understand"))
                .withShouldEndSession(false)
                .build();
    }
}
