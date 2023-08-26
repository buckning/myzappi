package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.Brand;
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

    public SetReminderHandler(ReminderServiceFactory reminderServiceFactory, UserZoneResolver userZoneResolver) {
        this.reminderServiceFactory = reminderServiceFactory;
        this.userZoneResolver = userZoneResolver;
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

        var zoneId = userZoneResolver.getZoneId(handlerInput);
        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        var reminderService = reminderServiceFactory.newReminderService(handlerInput);

        reminderService.createDailyRecurringReminder(LocalTime.of(23, 0), "test content", locale, zoneId);
        // TODO read the reminders from the DB and from Alexa. If there is already one set, update it in DB and Alexa

        // TODO if there is not one, create a recurring reminder, save the alert and schedule the SQS message

        return handlerInput.getResponseBuilder()
                .withSimpleCard(Brand.NAME, cardResponse(handlerInput, "reminder-set"))
                .withSpeech(voiceResponse(handlerInput, "reminder-set"))
                .build();
    }

    private boolean userNotGrantedPermissions(HandlerInput handlerInput) {
        var permissions = handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getPermissions();
        return permissions != null && permissions.getConsentToken() != null;
    }
}
