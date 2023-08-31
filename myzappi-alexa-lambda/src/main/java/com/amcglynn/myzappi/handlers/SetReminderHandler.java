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
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class SetReminderHandler implements RequestHandler {

    private ReminderServiceFactory reminderServiceFactory;
    private UserZoneResolver userZoneResolver;
    private AlexaToLwaLookUpRepository alexaToLwaLookUpRepository;
    private UserIdResolverFactory userIdResolverFactory;

    public SetReminderHandler(ReminderServiceFactory reminderServiceFactory, UserZoneResolver userZoneResolver, UserIdResolverFactory userIdResolverFactory, AlexaToLwaLookUpRepository alexaToLwaLookUpRepository) {
        this.reminderServiceFactory = reminderServiceFactory;
        this.userZoneResolver = userZoneResolver;
        this.alexaToLwaLookUpRepository = alexaToLwaLookUpRepository;
        this.userIdResolverFactory = userIdResolverFactory;
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
        var userLookup = alexaToLwaLookUpRepository.getLwaUserId(alexaUserId);
        var lwaUserFromRequest = userIdResolverFactory.newUserIdResolver(handlerInput).getUserId();

        userLookup.ifPresentOrElse(lwaUser -> {
            if (!lwaUser.equals(lwaUserFromRequest)) {
                log.info("LWA user from DB is {}, LWA user for token from request is {}, deleting old user from DB",
                        lwaUser, lwaUserFromRequest);
                alexaToLwaLookUpRepository.delete(alexaUserId);
                alexaToLwaLookUpRepository.write(alexaUserId, lwaUserFromRequest);
            }
        }, () -> alexaToLwaLookUpRepository.write(alexaUserId, lwaUserFromRequest));

        var zoneId = userZoneResolver.getZoneId(handlerInput);
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        var reminderService = reminderServiceFactory.newReminderService(handlerInput);

        var time = parseSlot(handlerInput, "time");
        var scheduledTime = LocalTime.parse(time.get());    // unsafe to call .get here usually but this has slot validation enabled so it is safe

        var alertToken = reminderService.createDailyRecurringReminder(handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getPermissions().getConsentToken(),
                scheduledTime, voiceResponse(handlerInput, "ev-not-connected"), locale, zoneId);

        log.info("Created new reminder: {} scheduling at {}", alertToken, scheduledTime);

        // TODO create a new scheduled job that runs 5 minutes before the reminder time

        return handlerInput.getResponseBuilder()
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "reminder-set"))
                .withSpeech(voiceResponse(handlerInput, "reminder-set"))
                .build();
    }

    private boolean userNotGrantedPermissions(HandlerInput handlerInput) {
        var permissions = handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getPermissions();
        return permissions != null && permissions.getConsentToken() != null;
    }

    private Optional<String> parseSlot(HandlerInput handlerInput, String slotName) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue(slotName);
    }
}
