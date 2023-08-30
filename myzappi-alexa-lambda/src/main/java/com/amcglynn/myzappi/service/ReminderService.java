package com.amcglynn.myzappi.service;

import com.amazon.ask.model.services.reminderManagement.AlertInfo;
import com.amazon.ask.model.services.reminderManagement.PushNotification;
import com.amazon.ask.model.services.reminderManagement.PushNotificationStatus;
import com.amazon.ask.model.services.reminderManagement.Recurrence;
import com.amazon.ask.model.services.reminderManagement.ReminderManagementServiceClient;
import com.amazon.ask.model.services.reminderManagement.ReminderRequest;
import com.amazon.ask.model.services.reminderManagement.SpokenInfo;
import com.amazon.ask.model.services.reminderManagement.SpokenText;
import com.amazon.ask.model.services.reminderManagement.Trigger;
import com.amazon.ask.model.services.reminderManagement.TriggerType;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.lwa.Reminder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Slf4j
public class ReminderService {

    private final ReminderManagementServiceClient reminderManagementServiceClient;
    private final LwaClient lwaClient;

    public ReminderService(ReminderManagementServiceClient reminderManagementServiceClient, LwaClient lwaClient) {
        this.reminderManagementServiceClient = reminderManagementServiceClient;
        this.lwaClient = lwaClient;
    }

    public String updateExisting(Reminder reminder, LocalTime reminderStartTime,
                                 String reminderText, Locale locale, ZoneId zoneId) {
        LocalDate now = LocalDate.now(zoneId);
        var scheduledStartTime = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(),
                reminderStartTime.getHour(), reminderStartTime.getMinute(), reminderStartTime.getSecond());

        ReminderRequest request = ReminderRequest.builder()
                .withRequestTime(OffsetDateTime.now())
                .withTrigger(Trigger.builder()
                        .withTimeZoneId(reminder.getTrigger().getTimeZoneId())
                        .withType(TriggerType.SCHEDULED_ABSOLUTE)
                        .withScheduledTime(scheduledStartTime)
                        .withRecurrence(Recurrence.builder()
                                .withStartDateTime(scheduledStartTime)
                                .withEndDateTime(scheduledStartTime.plusDays(5))
                                .withRecurrenceRules(List.of("FREQ=DAILY;BYHOUR=" + scheduledStartTime.getHour() +
                                        ";BYMINUTE=" + scheduledStartTime.getMinute() + ";BYSECOND=0"))
                                .build())
                        .build())
                .withAlertInfo(AlertInfo.builder()
                        .withSpokenInfo(SpokenInfo.builder()
                                .withContent(List.of(SpokenText.builder()
                                        .withLocale(locale.toLanguageTag())
                                        .withText(reminderText)
                                        .build()))
                                .build())
                        .build())
                .withPushNotification(PushNotification.builder().withStatus(PushNotificationStatus.ENABLED).build())
                .build();

        return reminderManagementServiceClient.updateReminder(reminder.getAlertToken(), request).getAlertToken();
    }

    public void handleReminderMessage(String accessToken, BooleanSupplier supplier) {
        // 1. check the existing reminder
        // 2. check if it is going to trigger in 5 mins or less
        // 3. If it is longer, calculate 5 minutes before the next reminder and schedule a message for that time

        var remindersResponse = lwaClient.getReminders("https://api.eu.amazonalexa.com", accessToken);

        var reminders = remindersResponse.getAlerts();

        if (reminders.isEmpty()) {
            return;
        }

        var reminderTime = reminders.get(0).getTrigger().getRecurrence().getStartTime();
        var timeZone = reminders.get(0).getTrigger().getTimeZoneId();

        var currentTime = LocalDateTime.now(ZoneId.of(timeZone));

        var nextCallBackTime = reminderTime.minusMinutes(5).toLocalDateTime();

        long minutesUntilReminder = Duration.between(currentTime, reminderTime).toMinutes();
        if (minutesUntilReminder < 5 && minutesUntilReminder >= 0) {
            if (supplier.getAsBoolean()) {
                delayReminderBy24Hours(reminders.get(0));
            }
            // delay callback for 24 hours - 5 minutes
            nextCallBackTime = reminderTime.plusDays(1).minusMinutes(5).toLocalDateTime();
        }

        scheduleCallback(nextCallBackTime);
    }

    private void scheduleCallback(LocalDateTime callbackTime) {
        log.info("Scheduling a callback for {}", callbackTime);
    }

    public String delayReminderBy24Hours(String accessToken) {
        log.info("Delay reminder by 24 hours");
        StringBuilder alertToken = new StringBuilder();
        var reminders = lwaClient.getReminders("https://api.eu.amazonalexa.com", accessToken);

        reminders.getAlerts().forEach(reminder -> {
            var oldReminderTime = reminder.getTrigger().getRecurrence().getStartTime().toLocalDateTime();
            var newReminderTime = oldReminderTime.plusDays(1);

            log.info("Updating reminder from {} to {}", oldReminderTime, newReminderTime);

            // need to change start time using something like this: reminder.getTrigger().getRecurrence().getStartTime().toLocalTime();
            ReminderRequest request = ReminderRequest.builder()
                    .withRequestTime(OffsetDateTime.now())
                    .withTrigger(Trigger.builder()
                            .withType(TriggerType.SCHEDULED_ABSOLUTE)
                            .withTimeZoneId(reminder.getTrigger().getTimeZoneId())
                            .withRecurrence(Recurrence.builder()
                                    .withStartDateTime(newReminderTime)
                                    .withRecurrenceRules(List.of("FREQ=DAILY;BYHOUR=" + newReminderTime.getHour() +
                                            ";BYMINUTE=" + newReminderTime.getMinute() + ";BYSECOND=0"))

                                    .build())
                            .build())
                    .withAlertInfo(AlertInfo.builder()
                            .withSpokenInfo(SpokenInfo.builder()
                                    .withContent(List.of(SpokenText.builder()
                                            .withLocale(reminder.getAlertInfo().getSpokenInfo().getContent().get(0).getLocale())
                                            .withText(reminder.getAlertInfo().getSpokenInfo().getContent().get(0).getText())
                                            .build()))
                                    .build())
                            .build())
                    .withPushNotification(PushNotification.builder().withStatus(PushNotificationStatus.ENABLED).build())
                    .build();
            log.info("Update reminder = {} request = {}", reminder.getAlertToken(), request);
            alertToken.insert(0, reminderManagementServiceClient.updateReminder(reminder.getAlertToken(), request).getAlertToken());
        });
        return alertToken.toString();
    }

    public void delayReminderBy24Hours(Reminder reminder) {
        var oldReminderTime = reminder.getTrigger().getRecurrence().getStartTime().toLocalDateTime();
        var newReminderTime = oldReminderTime.plusDays(1);

        log.info("Updating reminder from {} to {}", oldReminderTime, newReminderTime);

        // need to change start time using something like this: reminder.getTrigger().getRecurrence().getStartTime().toLocalTime();
        ReminderRequest request = ReminderRequest.builder()
                .withRequestTime(OffsetDateTime.now())
                .withTrigger(Trigger.builder()
                        .withType(TriggerType.SCHEDULED_ABSOLUTE)
                        .withTimeZoneId(reminder.getTrigger().getTimeZoneId())
                        .withRecurrence(Recurrence.builder()
                                .withStartDateTime(newReminderTime)
                                .withRecurrenceRules(List.of("FREQ=DAILY;BYHOUR=" + newReminderTime.getHour() +
                                        ";BYMINUTE=" + newReminderTime.getMinute() + ";BYSECOND=0"))

                                .build())
                        .build())
                .withAlertInfo(AlertInfo.builder()
                        .withSpokenInfo(SpokenInfo.builder()
                                .withContent(List.of(SpokenText.builder()
                                        .withLocale(reminder.getAlertInfo().getSpokenInfo().getContent().get(0).getLocale())
                                        .withText(reminder.getAlertInfo().getSpokenInfo().getContent().get(0).getText())
                                        .build()))
                                .build())
                        .build())
                .withPushNotification(PushNotification.builder().withStatus(PushNotificationStatus.ENABLED).build())
                .build();
        log.info("Update reminder = {} request = {}", reminder.getAlertToken(), request);
        reminderManagementServiceClient.updateReminder(reminder.getAlertToken(), request);
    }

    public String createDailyRecurringReminder(String accessToken, LocalTime reminderStartTime,
                                               String reminderText, Locale locale, ZoneId zoneId) {
        var reminders = lwaClient.getReminders("https://api.eu.amazonalexa.com", accessToken);

        if (reminders.getTotalCount() == 0) {
            log.info("No reminders exist, creating a new one");
            return createReminder(reminderStartTime, reminderText, locale, zoneId);
        } else {
            return updateExisting(reminders.getAlerts().get(0), reminderStartTime, reminderText, locale, zoneId);
        }
    }

    public String createReminder(LocalTime reminderStartTime, String reminderText, Locale locale, ZoneId zoneId) {
        log.info("Creating new reminder");
        LocalDate now = LocalDate.now(zoneId);
        var scheduledStartTime = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(),
                reminderStartTime.getHour(), reminderStartTime.getMinute(), reminderStartTime.getSecond());
        var request = ReminderRequest.builder()
                .withRequestTime(OffsetDateTime.now())
                .withTrigger(Trigger.builder()
                        .withTimeZoneId(zoneId.getId())
                        .withType(TriggerType.SCHEDULED_ABSOLUTE)
                        .withScheduledTime(scheduledStartTime)
                        .withRecurrence(Recurrence.builder()
                                .withStartDateTime(scheduledStartTime)
                                .withRecurrenceRules(List.of("FREQ=DAILY;BYHOUR=" + scheduledStartTime.getHour() +
                                        ";BYMINUTE=" + scheduledStartTime.getMinute() + ";BYSECOND=0"))

                                .build())
                        .build())
                .withAlertInfo(AlertInfo.builder()
                        .withSpokenInfo(SpokenInfo.builder()
                                .withContent(List.of(SpokenText.builder()
                                        .withLocale(locale.toLanguageTag())
                                        .withText(reminderText)
                                        .build()))
                                .build())
                        .build())
                .withPushNotification(PushNotification.builder().withStatus(PushNotificationStatus.ENABLED).build())
                .build();

        // note that this does not work on the simulator and will only work on a real device
        var response = reminderManagementServiceClient.createReminder(request);
        return response.getAlertToken();
    }

//    public void updateReminder(String alertToken) {
//        var scheduledStartTime = LocalDateTime.of(2023, 8, 27, 0, 0, 0);
//        ReminderRequest request = ReminderRequest.builder()
//                .withRequestTime(OffsetDateTime.now())
//                .withTrigger(Trigger.builder()
//                        .withTimeZoneId("Europe/Dublin")
//                        .withType(TriggerType.SCHEDULED_ABSOLUTE)
//                        .withScheduledTime(scheduledStartTime)
//                        .withRecurrence(Recurrence.builder()
//                                .withStartDateTime(scheduledStartTime)
//                                .withEndDateTime(scheduledStartTime.plus(5, ChronoUnit.DAYS))
//                                .withRecurrenceRules(List.of("FREQ=DAILY;BYHOUR=18;BYMINUTE=58;BYSECOND=0"))
//                                .build())
//                        .build())
//                .withAlertInfo(AlertInfo.builder()
//                        .withSpokenInfo(SpokenInfo.builder()
//                                .withContent(List.of(SpokenText.builder()
//                                        .withLocale("en-GB")
//                                        .withText("Your car is not plugged in")
//                                        .build()))
//                                .build())
//                        .build())
//                .withPushNotification(PushNotification.builder().withStatus(PushNotificationStatus.ENABLED).build())
//                .build();
//
//        log.info("Updating reminder from event");
//        handlerInput.getServiceClientFactory().getReminderManagementService().updateReminder(alertToken, request);
//    }
}
