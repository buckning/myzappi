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

@Slf4j
public class ReminderService {

    private final ReminderManagementServiceClient reminderManagementServiceClient;
    private final LwaClient lwaClient;
    private final SchedulerService schedulerService;

    public ReminderService(ReminderManagementServiceClient reminderManagementServiceClient, LwaClient lwaClient,
                           SchedulerService schedulerService) {
        this.reminderManagementServiceClient = reminderManagementServiceClient;
        this.lwaClient = lwaClient;
        this.schedulerService = schedulerService;
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

        var reminderTime = LocalDateTime.parse(reminders.get(0).getTrigger().getScheduledTime());
        var timeZone = reminders.get(0).getTrigger().getTimeZoneId();

        var currentTime = LocalDateTime.now(ZoneId.of(timeZone));

        var nextCallBackTime = getNextOccurrence(reminderTime);

        long minutesUntilReminder = Duration.between(currentTime, reminderTime).toMinutes();

        log.info("reminderTime = {}", reminderTime);
        log.info("currentTime = {}", currentTime);
        log.info("Distance until reminder = {} mins", minutesUntilReminder);
        if (minutesUntilReminder < 5 && minutesUntilReminder >= 0) {
            if (supplier.getAsBoolean()) {
                log.info("Car is plugged in, delaying for 24 hours");
                delayReminderBy24Hours(reminders.get(0));
            }
            // delay callback for 24 hours - 5 minutes
            nextCallBackTime = reminderTime.plusDays(1).minusMinutes(5);
            log.info("setting nextCallBackTime = {}", nextCallBackTime);
        }

        // best thing to do here is get the recurrence time and build up the local time. Then get the day for the next occurrence of that time, today or tomorrow.
        // Can't rely on scheduledTime because our updates don't affect it.
        // When we update via the app, it updates scheduledTime but not startDateTime
        scheduleCallback(nextCallBackTime);
    }

    private LocalDateTime getNextOccurrence(LocalDateTime reminderDateTime) {
        var alertDateTime = reminderDateTime.minusMinutes(5);
        var currentDateTime = LocalDateTime.now();

        var reminderTime = alertDateTime.toLocalTime();

        if (alertDateTime.isBefore(currentDateTime)) {
            return LocalDateTime.of(currentDateTime.toLocalDate().plusDays(1), reminderTime);
        }

        return alertDateTime;
    }

    private void scheduleCallback(LocalDateTime callbackTime) {
        log.info("Scheduling a callback for {}", callbackTime);
        schedulerService.schedule(callbackTime);
    }

    public void delayReminderBy24Hours(Reminder reminder) {
        var oldReminderTime = LocalDateTime.parse(reminder.getTrigger().getScheduledTime());
        var newReminderTime = oldReminderTime.plusDays(1);
        var locale = Locale.forLanguageTag(reminder.getAlertInfo().getSpokenInfo().getContent().get(0).getLocale());
        var reminderText = reminder.getAlertInfo().getSpokenInfo().getContent().get(0).getText();
        var zoneId = ZoneId.of(reminder.getTrigger().getTimeZoneId());

        log.info("Updating reminder from {} to {}", oldReminderTime, newReminderTime);

        var request = createReminderRequest(newReminderTime, zoneId, reminderText, locale);
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
        var request = createReminderRequest(scheduledStartTime, zoneId, reminderText, locale);

        // note that this does not work on the simulator and will only work on a real device
        var response = reminderManagementServiceClient.createReminder(request);
        return response.getAlertToken();
    }

    private ReminderRequest createReminderRequest(LocalDateTime startTime, ZoneId zoneId, String reminderText,
                                                  Locale locale) {
        return ReminderRequest.builder()
                .withRequestTime(OffsetDateTime.now())
                .withTrigger(Trigger.builder()
                        .withTimeZoneId(zoneId.getId())
                        .withType(TriggerType.SCHEDULED_ABSOLUTE)
                        .withScheduledTime(startTime)
                        .withRecurrence(Recurrence.builder()
                                .withStartDateTime(startTime)
                                .withRecurrenceRules(List.of("FREQ=DAILY;BYHOUR=" + startTime.getHour() +
                                        ";BYMINUTE=" + startTime.getMinute() + ";BYSECOND=0"))

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
    }
}
