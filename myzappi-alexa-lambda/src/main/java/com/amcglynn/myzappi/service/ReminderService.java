package com.amcglynn.myzappi.service;

import com.amazon.ask.model.services.reminderManagement.AlertInfo;
import com.amazon.ask.model.services.reminderManagement.GetRemindersResponse;
import com.amazon.ask.model.services.reminderManagement.PushNotification;
import com.amazon.ask.model.services.reminderManagement.PushNotificationStatus;
import com.amazon.ask.model.services.reminderManagement.Recurrence;
import com.amazon.ask.model.services.reminderManagement.RecurrenceFreq;
import com.amazon.ask.model.services.reminderManagement.ReminderManagementServiceClient;
import com.amazon.ask.model.services.reminderManagement.ReminderRequest;
import com.amazon.ask.model.services.reminderManagement.SpokenInfo;
import com.amazon.ask.model.services.reminderManagement.SpokenText;
import com.amazon.ask.model.services.reminderManagement.Trigger;
import com.amazon.ask.model.services.reminderManagement.TriggerType;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.lwa.Reminders;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Slf4j
public class ReminderService {

    private final ReminderManagementServiceClient reminderManagementServiceClient;
    private final LwaClient lwaClient;

    public ReminderService(ReminderManagementServiceClient reminderManagementServiceClient, LwaClient lwaClient) {
        this.reminderManagementServiceClient = reminderManagementServiceClient;
        this.lwaClient = lwaClient;
    }

    public void update(String accessToken) {
        var reminderOpt = lwaClient.getReminders("https://api.eu.amazonalexa.com", accessToken);

        reminderOpt.get().getAlerts().forEach(reminder -> {
            var scheduledTime = LocalDateTime.parse(reminder.getTrigger().getScheduledTime()).plus(1, ChronoUnit.DAYS);
            log.info("Updating reminder {} by 24 hours to {}", reminder, scheduledTime);
            ReminderRequest request = ReminderRequest.builder()
                    .withRequestTime(OffsetDateTime.now())
                    .withTrigger(Trigger.builder()
                            .withTimeZoneId(reminder.getTrigger().getTimeZoneId())
                            .withType(TriggerType.SCHEDULED_ABSOLUTE)
                            .withScheduledTime(scheduledTime)
                            .withRecurrence(Recurrence.builder()
                                    .withStartDateTime(scheduledTime)
                                    .withEndDateTime(scheduledTime.plus(5, ChronoUnit.DAYS))
                                    .withRecurrenceRules(List.of("FREQ=DAILY;BYHOUR=" + scheduledTime.getHour() +
                                            ";BYMINUTE=" + scheduledTime.getMinute() + ";BYSECOND=0"))
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

            reminderManagementServiceClient.updateReminder(reminder.getAlertToken(), request);
        });
    }

    public String createDailyRecurringReminder(LocalTime reminderStartTime, String reminderText, Locale locale, ZoneId zoneId) {
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
                                .withFreq(RecurrenceFreq.DAILY)
                                .withInterval(1)
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
