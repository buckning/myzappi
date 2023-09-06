package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.dal.AlexaToLwaLookUpRepository;
import com.amcglynn.myzappi.service.ReminderServiceFactory;
import com.amcglynn.myzappi.service.SchedulerService;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class SetReminderHandler implements RequestHandler {

    private final ReminderServiceFactory reminderServiceFactory;
    private final UserZoneResolver userZoneResolver;
    private final AlexaToLwaLookUpRepository alexaToLwaLookUpRepository;
    private final UserIdResolverFactory userIdResolverFactory;
    private final SchedulerService schedulerService;
    @Setter(AccessLevel.PACKAGE)
    private Supplier<LocalDateTime> localDateTimeSupplier;

    public SetReminderHandler(ReminderServiceFactory reminderServiceFactory, UserZoneResolver userZoneResolver, UserIdResolverFactory userIdResolverFactory, AlexaToLwaLookUpRepository alexaToLwaLookUpRepository, SchedulerService schedulerService) {
        this.reminderServiceFactory = reminderServiceFactory;
        this.userZoneResolver = userZoneResolver;
        this.alexaToLwaLookUpRepository = alexaToLwaLookUpRepository;
        this.userIdResolverFactory = userIdResolverFactory;
        this.schedulerService = schedulerService;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("SetReminder"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        if (!userNotGrantedPermissions(handlerInput)) {
            return handlerInput.getResponseBuilder()
                    .withAskForPermissionsConsentCard(List.of("alexa::alerts:reminders:skill:readwrite"))
                    .withSpeech(voiceResponse(handlerInput, "grant-reminder-permission"))
                    .build();
        }

        var alexaUserId = handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getUserId();
        var userDetails = alexaToLwaLookUpRepository.read(alexaUserId);
        var lwaUserFromRequest = userIdResolverFactory.newUserIdResolver(handlerInput).getUserId();
        var zoneId = userZoneResolver.getZoneId(handlerInput);

        userDetails.ifPresentOrElse(user -> {
            var lwaUser = user.getLwaUserId();
            if (!lwaUser.equals(lwaUserFromRequest)) {
                log.info("LWA user from DB is {}, LWA user for token from request is {}, deleting old user from DB",
                        lwaUser, lwaUserFromRequest);
                alexaToLwaLookUpRepository.delete(alexaUserId);
                alexaToLwaLookUpRepository.write(alexaUserId, lwaUserFromRequest, zoneId.getId());
            }
        }, () -> alexaToLwaLookUpRepository.write(alexaUserId, lwaUserFromRequest, zoneId.getId()));

        var currentDateTime = getLocalDateTime(zoneId);
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        var reminderService = reminderServiceFactory.newReminderService(handlerInput);

        var time = parseTimeSlot(handlerInput);
        var scheduledTime = LocalTime.parse(time.get());    // unsafe to call .get here usually but this has slot validation enabled so it is safe

        var alertToken = reminderService.createDailyRecurringReminder(handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getPermissions().getConsentToken(),
                scheduledTime, voiceResponse(handlerInput, "ev-not-connected"), locale, zoneId);

        var scheduledCallback = getNextOccurrence(LocalDateTime.of(currentDateTime.toLocalDate(), scheduledTime));

        schedulerService.schedule(scheduledCallback, alexaUserId, zoneId);

        log.info("Created new reminder: {} scheduling at {}, scheduling callback at {}", alertToken, scheduledTime, scheduledCallback);

        return handlerInput.getResponseBuilder()
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "reminder-set"))
                .withSpeech(voiceResponse(handlerInput, "reminder-set"))
                .build();
    }

    private LocalDateTime getNextOccurrence(LocalDateTime reminderDateTime) {
        var alertDateTime = reminderDateTime.minusMinutes(5);
        var currentDateTime = localDateTimeSupplier.get();

        var reminderTime = alertDateTime.toLocalTime();

        if (alertDateTime.isBefore(currentDateTime)) {
            return LocalDateTime.of(currentDateTime.toLocalDate().plusDays(1), reminderTime);
        }

        return alertDateTime;
    }

    private LocalDateTime getLocalDateTime(ZoneId zoneId) {
        if (localDateTimeSupplier == null) {
            localDateTimeSupplier = () -> LocalDateTime.now(zoneId);
        }

        return localDateTimeSupplier.get();
    }

    private boolean userNotGrantedPermissions(HandlerInput handlerInput) {
        var permissions = handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getPermissions();
        return permissions != null && permissions.getConsentToken() != null;
    }

    private Optional<String> parseTimeSlot(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue("time");
    }
}
