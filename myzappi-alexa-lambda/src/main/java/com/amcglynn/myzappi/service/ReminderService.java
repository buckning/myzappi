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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Some observations about Alexa reminders API. Don't use startDateTime to schedule the time of recurring reminders.
 * Use recurrenceRules to set the time only. Don't set the time in startDateTime. I found that sometimes the reminders
 * get set on the wrong date because of some combination of the two.
 * Also don't bother setting the scheduledTime because the API will just override what you send into it anyway.
 * scheduledTime seems like it is the date and time that the reminder will trigger regardless of the startDateTime.
 * scheduledTime will always show the next occurrence of the reminder.
 * <br>
 * Because of all of these points, the strategy is to set the startDateTime to the required date and the time is set to 00:00:00
 * or 1 minute before the required time.
 * ScheduledTime is not to be set in any request but it can be used in the response.
 */
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
                        .withRecurrence(Recurrence.builder()
                                .withStartDateTime(scheduledStartTime.minusMinutes(1))  // minus 1 minute so that the scheduledTime will not be pushed to the next day. See description at top of class for more detail
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

    public void handleReminderMessage(String accessToken, String alexaUserId, String zoneId, BooleanSupplier supplier) {
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
        scheduleCallback(nextCallBackTime, alexaUserId, zoneId);
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

    private void scheduleCallback(LocalDateTime callbackTime, String alexaUserId, String zoneId) {
        log.info("Scheduling a callback for {} at {} at zone {}", alexaUserId, callbackTime, zoneId);
        schedulerService.schedule(callbackTime, alexaUserId, ZoneId.of(zoneId));
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
        LocalDate now = LocalDate.now(zoneId);
        var scheduledStartTime = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(),
                reminderStartTime.getHour(), reminderStartTime.getMinute(), reminderStartTime.getSecond());
        log.info("Creating new reminder with text {} at time {}", reminderText, scheduledStartTime);
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
                        .withRecurrence(Recurrence.builder()
                                .withStartDateTime(startTime.minusMinutes(1))   // minus 1 minute so that the scheduledTime will not be pushed to the next day. See description at top of class for more detail
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
