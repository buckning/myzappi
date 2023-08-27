package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.ServiceException;
import com.amazon.ask.model.services.reminderManagement.AlertInfo;
import com.amazon.ask.model.services.reminderManagement.PushNotification;
import com.amazon.ask.model.services.reminderManagement.PushNotificationStatus;
import com.amazon.ask.model.services.reminderManagement.Recurrence;
import com.amazon.ask.model.services.reminderManagement.RecurrenceFreq;
import com.amazon.ask.model.services.reminderManagement.ReminderRequest;
import com.amazon.ask.model.services.reminderManagement.SpokenInfo;
import com.amazon.ask.model.services.reminderManagement.SpokenText;
import com.amazon.ask.model.services.reminderManagement.Trigger;
import com.amazon.ask.model.services.reminderManagement.TriggerType;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.myzappi.service.ReminderService;
import com.amcglynn.myzappi.service.ReminderServiceFactory;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * This is invoked by Alexa message events and not through a voice intent.
 */
@Slf4j
public class MessageReceivedHandler implements RequestHandler {

    private ReminderServiceFactory reminderServiceFactory;

    public MessageReceivedHandler(ReminderServiceFactory reminderServiceFactory) {
        this.reminderServiceFactory = reminderServiceFactory;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        log.info("Handler request type = {}", handlerInput.getRequest().getType());
        return "Messaging.MessageReceived".equals(handlerInput.getRequest().getType());
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
//        log.info("data = {}", handlerInput.getRequestEnvelope());
        var reminderService = reminderServiceFactory.newReminderService(handlerInput);
        try {
//            var reminders = handlerInput.getServiceClientFactory().getReminderManagementService().getReminder("d8337972-23eb-4036-a83d-4a35bc9a0104");

            log.info("Updating reminder by 24 hours");
            reminderService.update(handlerInput.getRequestEnvelope().getContext().getSystem().getUser().getPermissions().getConsentToken());

//            var scheduledStartTime = LocalDateTime.of(2023, 8, 27, 0, 0, 0);
//            ReminderRequest request = ReminderRequest.builder()
//                    .withRequestTime(OffsetDateTime.now())
//                    .withTrigger(Trigger.builder()
//                            .withTimeZoneId("Europe/Dublin")
//                            .withType(TriggerType.SCHEDULED_ABSOLUTE)
//                            .withScheduledTime(scheduledStartTime)
//                            .withRecurrence(Recurrence.builder()
//                                    .withStartDateTime(scheduledStartTime)
//                                    .withEndDateTime(scheduledStartTime.plus(5, ChronoUnit.DAYS))
//                                    .withRecurrenceRules(List.of("FREQ=DAILY;BYHOUR=18;BYMINUTE=58;BYSECOND=0"))
//                                    .build())
//                            .build())
//                    .withAlertInfo(AlertInfo.builder()
//                            .withSpokenInfo(SpokenInfo.builder()
//                                    .withContent(List.of(SpokenText.builder()
//                                            .withLocale("en-GB")
//                                            .withText("Your car is not plugged in")
//                                            .build()))
//                                    .build())
//                            .build())
//                    .withPushNotification(PushNotification.builder().withStatus(PushNotificationStatus.ENABLED).build())
//                    .build();
//
//            log.info("Updating reminder from event");
//            handlerInput.getServiceClientFactory().getReminderManagementService().updateReminder("d8337972-23eb-4036-a83d-4a35bc9a0104", request);
//            log.info("Reminder update for 2 hours");
        } catch (ServiceException e) {
            log.error("Error when setting reminder update {}", e.getBody());
        } catch (Exception e) {
            log.error("Unexpected error ", e);
        }

        return handlerInput.getResponseBuilder()
                .build();
    }
}