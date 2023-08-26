package com.amcglynn.myzappi.service;

import com.amazon.ask.model.services.reminderManagement.AlertInfo;
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
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Slf4j
public class ReminderService {

    private final ReminderManagementServiceClient reminderManagementServiceClient;

    public ReminderService(ReminderManagementServiceClient reminderManagementServiceClient) {
        this.reminderManagementServiceClient = reminderManagementServiceClient;
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
}
