package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.RenderDocumentDirective;
import com.amazon.ask.request.RequestHelper;
import com.amazon.ask.response.ResponseBuilder;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.units.KiloWatt;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.Clock;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.exception.InvalidScheduleException;
import com.amcglynn.myzappi.mappers.AlexaZappiChargeModeMapper;
import com.amcglynn.myzappi.service.ControlPanelBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;
import static com.amcglynn.myzappi.RequestAttributes.getZappiServiceOrThrow;

@Slf4j
public class ScheduleJobHandler implements RequestHandler {

    private final ScheduleService scheduleService;
    private final UserIdResolverFactory userIdResolverFactory;
    private final UserZoneResolver userZoneResolver;
    private final AlexaZappiChargeModeMapper mapper;
    private final Clock clock;

    public ScheduleJobHandler(ScheduleService scheduleService, UserIdResolverFactory userIdResolverFactory,
                              UserZoneResolver userZoneResolver, Clock clock) {
        this.scheduleService = scheduleService;
        this.userIdResolverFactory = userIdResolverFactory;
        this.userZoneResolver = userZoneResolver;
        mapper = new AlexaZappiChargeModeMapper();
        this.clock = clock;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("ScheduleJob"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var scheduleTime = parseDateTime(handlerInput);
        var userIdResolver = userIdResolverFactory.newUserIdResolver(handlerInput);
        var zoneId = userZoneResolver.getZoneId(handlerInput);

        var scheduleAction = validateScheduleTypeIsConfigured(handlerInput);
        var startDateTime = LocalDateTime.of(clock.localDate(zoneId), scheduleTime);

        var schedule = scheduleService.createSchedule(UserId.from(userIdResolver.getUserId()), Schedule.builder()
                .zoneId(zoneId)
                .startDateTime(getNextOccurrence(startDateTime, clock.localDateTime(zoneId)))
                .action(scheduleAction)
                .build());

        var responseBuilder = handlerInput.getResponseBuilder();

        if (hasDisplayInterface(handlerInput)) {
            responseBuilder.addDirective(buildAplResponse(schedule));
        }

        return responseBuilder
                .withShouldEndSession(false)
                .withSpeech(voiceResponse(handlerInput, "scheduled-job"))
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "scheduled-job"))
                .build();
    }

    private boolean hasDisplayInterface(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSupportedInterfaces()
                .getAlexaPresentationAPL() != null;
    }

    public RenderDocumentDirective buildAplResponse(Schedule schedule) {
        return RenderDocumentDirective.builder()
                .withToken("zappidaysummaryToken")
                .withDocument(buildDocument(schedule))
                .build();
    }

    @SneakyThrows
    private Map<String, Object> buildDocument(Schedule schedule) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> documentMapType = new TypeReference<>() {
        };

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("apl/create-one-time-schedule.json");

        var contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        contents = contents.replace("${payload.scheduleStartTime}", LocalTime.of(schedule.getStartDateTime().getHour(), schedule.getStartDateTime().getMinute()).toString());
        contents = contents.replace("${payload.scheduleStartDate}", LocalDate.from(schedule.getStartDateTime()).toString());
        contents = contents.replace("${payload.scheduleActionType}", schedule.getAction().getType());
        contents = contents.replace("${payload.scheduleActionValue}", schedule.getAction().getValue());
        contents = contents.replace("${payload.scheduleId}", schedule.getId());

        return mapper.readValue(contents, documentMapType);
    }

    private ScheduleAction validateScheduleTypeIsConfigured(HandlerInput handlerInput) {
        var duration = parseDurationSlot(handlerInput);
        var kwh = parseKiloWattHourSlot(handlerInput);
        var chargeMode = parseChargeMode(handlerInput);
        var boostEndTime = parseSlot(handlerInput, "boostEndTime");

        if (duration.isPresent()) {
            return ScheduleAction.builder()
                    .type("setBoostFor")
                    .value(duration.get().toString())
                    .build();
        } else if (kwh.isPresent()) {
            return ScheduleAction.builder()
                    .type("setBoostKwh")
                    .value(kwh.get().toString())
                    .build();
        } else if (chargeMode.isPresent()) {
            return ScheduleAction.builder()
                    .type("setChargeMode")
                    .value(chargeMode.get().toString())
                    .build();
        } else if (boostEndTime.isPresent()) {
            return ScheduleAction.builder()
                    .type("setBoostUntil")
                    .value(boostEndTime.get())
                    .build();
        }

        throw new InvalidScheduleException("No valid value provided");
    }

    private LocalDateTime getNextOccurrence(LocalDateTime scheduleStartTime, LocalDateTime currentDateTime) {
        if (scheduleStartTime.isBefore(currentDateTime)) {
            return LocalDateTime.of(currentDateTime.toLocalDate().plusDays(1), scheduleStartTime.toLocalTime());
        }

        return scheduleStartTime;
    }

    private Optional<Duration> parseDurationSlot(HandlerInput handlerInput) {
        return parseSlot(handlerInput, "boostDuration").map(Duration::parse);
    }

    private Optional<KiloWattHour> parseKiloWattHourSlot(HandlerInput handlerInput) {
        return parseSlot(handlerInput, "boostKwh").map(Double::parseDouble).map(KiloWattHour::new);
    }

    private Optional<ZappiChargeMode> parseChargeMode(HandlerInput handlerInput) {
        // alexa will do the validation, so it is safe to parse the charge mode
        return parseSlot(handlerInput, "chargeMode")
                .map(chargeMode -> mapper.getZappiChargeMode(chargeMode.toLowerCase()).get());
    }

    private Optional<String> parseSlot(HandlerInput handlerInput, String slotName) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue(slotName);
    }

    private LocalTime parseDateTime(HandlerInput handlerInput) {
        // alexa will do the validation, so it is safe to parse the time
        return LocalTime.parse(parseSlot(handlerInput, "scheduleTime").get());
    }
}
