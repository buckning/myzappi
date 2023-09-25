package com.amcglynn.myzappi.service;

import com.amazon.ask.model.services.reminderManagement.AlertInfo;
import com.amazon.ask.model.services.reminderManagement.PushNotification;
import com.amazon.ask.model.services.reminderManagement.PushNotificationStatus;
import com.amazon.ask.model.services.reminderManagement.Recurrence;
import com.amazon.ask.model.services.reminderManagement.ReminderManagementServiceClient;
import com.amazon.ask.model.services.reminderManagement.ReminderRequest;
import com.amazon.ask.model.services.reminderManagement.ReminderResponse;
import com.amazon.ask.model.services.reminderManagement.SpokenInfo;
import com.amazon.ask.model.services.reminderManagement.SpokenText;
import com.amazon.ask.model.services.reminderManagement.Trigger;
import com.amazon.ask.model.services.reminderManagement.TriggerType;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.lwa.Reminder;
import com.amcglynn.myzappi.core.service.Clock;
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
    private final Clock clock;

    public ReminderService(ReminderManagementServiceClient reminderManagementServiceClient, LwaClient lwaClient,
                           SchedulerService schedulerService, Clock clock) {
        this.reminderManagementServiceClient = reminderManagementServiceClient;
        this.lwaClient = lwaClient;
        this.schedulerService = schedulerService;
        this.clock = clock;
    }

    public String createDailyRecurringReminder(String accessToken, LocalTime reminderStartTime,
                                               String reminderText, Locale locale, ZoneId zoneId) {
        var reminders = lwaClient.getReminders("https://api.eu.amazonalexa.com", accessToken);

        LocalDate now = clock.localDate(zoneId);
        var scheduledStartTime = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(),
                reminderStartTime.getHour(), reminderStartTime.getMinute(), reminderStartTime.getSecond());

        var request = createReminderRequest(scheduledStartTime, zoneId, reminderText, locale);

        ReminderResponse reminder;
        if (reminders.getTotalCount() == 0) {
            log.info("Creating new reminder with text {} at time {}", reminderText, scheduledStartTime);
            reminder = reminderManagementServiceClient.createReminder(request);
        } else {
            log.info("Updating existing reminder with text {} at time {}", reminderText, scheduledStartTime);
            reminder = reminderManagementServiceClient.updateReminder(reminders.getAlerts().get(0).getAlertToken(), request);
        }

        return reminder.getAlertToken();
    }

    /**
     * Handle a reminder message update. This comes in from a Post Alexa Message handler (eventbridge scheduler).
     * This reads the existing reminder and reschedules it based on the supplier. If the supplier returns true, the
     * the reminder will be delayed until the next day, otherwise it will play for the user on their Alexa device.
     * So if the supplier checks to see if the user's E.V. is connected, it will not play the reminder to tell the user
     * that their E.V. is not connected.
     * @param accessToken Alexa consent token, used to read messages
     * @param alexaUserId userId of the Alexa user
     * @param zoneId time zone of the user
     * @param supplier any boolean supplier. This controls whether to delay the reminder or not.
     */
    public void handleReminderMessage(String accessToken, String alexaUserId, String zoneId, BooleanSupplier supplier) {
        var remindersResponse = lwaClient.getReminders("https://api.eu.amazonalexa.com", accessToken);

        var reminders = remindersResponse.getAlerts();

        if (reminders.isEmpty()) {
            log.info("No reminders found for user {}", alexaUserId);
            return;
        }

        log.info("Found {} reminders for user {}", reminders.size(), alexaUserId);

        var reminderScheduledTime = LocalDateTime.parse(reminders.get(0).getTrigger().getScheduledTime());
        var currentDateTime = clock.localDateTime(ZoneId.of(reminders.get(0).getTrigger().getTimeZoneId()));

        var nextReminderDateTime = getNextReminderDateTime(reminderScheduledTime, currentDateTime);

        long minutesUntilReminder = Duration.between(currentDateTime.toLocalTime(), reminderScheduledTime.toLocalTime()).toMinutes();

        log.info("reminderScheduledTime = {}", reminderScheduledTime);
        log.info("currentDateTime = {}", currentDateTime);
        log.info("Distance until reminder = {} mins", minutesUntilReminder);
        if (minutesUntilReminder < 5 && minutesUntilReminder >= 0) {
            if (supplier.getAsBoolean()) {
                log.info("Car is plugged in, delaying for 24 hours");
                delayReminder(reminders.get(0), nextReminderDateTime);
            }
            log.info("setting nextReminderDateTime = {}", nextReminderDateTime);
        }

        scheduleCallback(nextReminderDateTime.minusMinutes(5), alexaUserId, zoneId);
    }

    /**
     * Get the date and time for the next reminder. This is depending on when the "alertDateTime", which is the threshold
     * where the date moves from one date to another. If the current date time is before the threshold, the reminder time
     * still hasn't happened yet so the reminderDateTime is returned since it is still valid.
     * If the current time is after the threshold, the reminder date has to move into the next day from now.
     * @param reminderDateTime reminder date and time (reminder scheduledTime)
     * @param currentDateTime current date and time for the user.
     * @return the next date and time for the reminder. This can be at most 24 hours + 5 minutes
     */
    private LocalDateTime getNextReminderDateTime(LocalDateTime reminderDateTime, LocalDateTime currentDateTime) {
        var alertDateTime = reminderDateTime.minusMinutes(5);

        if (alertDateTime.isBefore(currentDateTime)) {
            return LocalDateTime.of(currentDateTime.toLocalDate().plusDays(1), reminderDateTime.toLocalTime());
        }

        return reminderDateTime;
    }

    private void scheduleCallback(LocalDateTime callbackTime, String alexaUserId, String zoneId) {
        log.info("Scheduling a callback for {} at {} at zone {}", alexaUserId, callbackTime, zoneId);
        schedulerService.schedule(callbackTime, alexaUserId, ZoneId.of(zoneId));
    }

    /**
     * Delay a reminder until a newReminderDateTime. This keeps the same reminder text, locale and time zone, only the
     * recurring time and start time are modified.
     * @param reminder reminder being updated
     * @param newReminderDateTime date and time of when the reminder is being changed to
     */
    private void delayReminder(Reminder reminder, LocalDateTime newReminderDateTime) {
        var locale = Locale.forLanguageTag(reminder.getAlertInfo().getSpokenInfo().getContent().get(0).getLocale());
        var reminderText = reminder.getAlertInfo().getSpokenInfo().getContent().get(0).getText();
        var zoneId = ZoneId.of(reminder.getTrigger().getTimeZoneId());

        var request = createReminderRequest(newReminderDateTime, zoneId, reminderText, locale);

        reminderManagementServiceClient.updateReminder(reminder.getAlertToken(), request);
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
