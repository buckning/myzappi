package com.amcglynn.myzappi.service;

import com.amazon.ask.model.services.reminderManagement.PushNotification;
import com.amazon.ask.model.services.reminderManagement.PushNotificationStatus;
import com.amazon.ask.model.services.reminderManagement.RecurrenceFreq;
import com.amazon.ask.model.services.reminderManagement.ReminderManagementServiceClient;
import com.amazon.ask.model.services.reminderManagement.ReminderRequest;
import com.amazon.ask.model.services.reminderManagement.ReminderResponse;
import com.amazon.ask.model.services.reminderManagement.TriggerType;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.lwa.Reminder;
import com.amcglynn.lwa.Reminders;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReminderServiceTest {

    private ReminderService reminderService;
    @Mock
    private ReminderManagementServiceClient mockReminderClient;
    @Mock
    private LwaClient mockLwaClient;

    @Captor
    private ArgumentCaptor<ReminderRequest> reminderRequestCaptor;

    @BeforeEach
    void setUp() {
        when(mockReminderClient.createReminder(any())).thenReturn(ReminderResponse.builder().withAlertToken("testAlertToken").build());
        this.reminderService = new ReminderService(mockReminderClient, mockLwaClient);
    }

    @Test
    void testCreateRecurringReminderCreatesNewReminderWhenNonExist() {
        when(mockLwaClient.getReminders(anyString(), anyString())).thenReturn(new Reminders(0, List.of()));
        var response = reminderService.createDailyRecurringReminder("testAccessToken", LocalTime.of(23, 0), "Test content",
                Locale.forLanguageTag("en-GB"), ZoneId.of("Europe/Dublin"));

        assertThat(response).isNotNull().isEqualTo("testAlertToken");
        verify(mockReminderClient).createReminder(reminderRequestCaptor.capture());

        var reminderRequest = reminderRequestCaptor.getValue();
        verifyContent(reminderRequest);
        verifyTrigger(reminderRequest);
        assertThat(reminderRequest.getPushNotification())
                .isEqualTo(PushNotification.builder().withStatus(PushNotificationStatus.ENABLED).build());
    }

    @Test
    void testCreateRecurringReminderUpdatesExistingReminderWhenOneAlreadyExist() {
        when(mockLwaClient.getReminders(anyString(), anyString())).thenReturn(getReminders());
        when(mockReminderClient.updateReminder(any(), any()))
                .thenReturn(ReminderResponse.builder().withAlertToken("testAlertToken").build());

        var response = reminderService.createDailyRecurringReminder("testAccessToken", LocalTime.of(23, 0), "Test content",
                Locale.forLanguageTag("en-GB"), ZoneId.of("Europe/Dublin"));

        assertThat(response).isNotNull().isEqualTo("testAlertToken");
        verify(mockReminderClient).updateReminder(eq("testAlertToken"), reminderRequestCaptor.capture());

        var reminderRequest = reminderRequestCaptor.getValue();
        verifyContent(reminderRequest);
        verifyTriggerUpdate(reminderRequest);
        assertThat(reminderRequest.getPushNotification())
                .isEqualTo(PushNotification.builder().withStatus(PushNotificationStatus.ENABLED).build());
    }

    private Reminders getReminders() {
        return new Reminders(1, List.of(Reminder.builder()
                .alertToken("testAlertToken")
                .createdTime("2023-08-27T07:58:30.990Z")
                .updatedTime("2023-08-27T07:58:31.211Z")
                .trigger(Reminder.Trigger.builder()
                        .type("SCHEDULED_ABSOLUTE")
                        .scheduledTime("2023-08-27T23:00:00.000")
                        .timeZoneId("Europe/Dublin")
                        .offsetInSeconds(0)
                        .recurrence(Reminder.Recurrence.builder()
                                .freq("DAILY")
                                .startDateTime("")
                                .endDateTime("")
                                .recurrenceRules(List.of())
                                .build())
                        .build())
                .alertInfo(Reminder.AlertInfo.builder()
                        .spokenInfo(Reminder.AlertInfo.SpokenInfo.builder()
                                .content(List.of(Reminder.AlertInfo.SpokenInfo.Content.builder()
                                        .locale("en-GB")
                                        .text("test content")
                                        .build()))
                                .build())
                        .build())
                .build()));
    }

    private void verifyContent(ReminderRequest reminderRequest) {
        assertThat(reminderRequest).isNotNull();
        var alertInfo = reminderRequest.getAlertInfo();
        assertThat(alertInfo).isNotNull();
        var content = alertInfo.getSpokenInfo().getContent();
        assertThat(content).isNotNull().hasSize(1);
        assertThat(content.get(0).getLocale()).isEqualTo("en-GB");
        assertThat(content.get(0).getText()).isEqualTo("Test content");
    }

    private void verifyTrigger(ReminderRequest reminderRequest) {
        var trigger = reminderRequest.getTrigger();
        assertThat(trigger).isNotNull();
        assertThat(trigger.getTimeZoneId()).isEqualTo("Europe/Dublin");
        assertThat(trigger.getType()).isEqualTo(TriggerType.SCHEDULED_ABSOLUTE);
        var localDate = LocalDate.now(ZoneId.of("Europe/Dublin"));
        assertThat(trigger.getScheduledTime()).isEqualTo(LocalDateTime.of(localDate, LocalTime.of(23, 0, 0)));

        var recurrence = trigger.getRecurrence();
        assertThat(recurrence.getFreq()).isEqualTo(RecurrenceFreq.DAILY);
        assertThat(recurrence.getInterval()).isEqualTo(1);
    }

    private void verifyTriggerUpdate(ReminderRequest reminderRequest) {
        var trigger = reminderRequest.getTrigger();
        assertThat(trigger).isNotNull();
        assertThat(trigger.getTimeZoneId()).isEqualTo("Europe/Dublin");
        assertThat(trigger.getType()).isEqualTo(TriggerType.SCHEDULED_ABSOLUTE);
        var localDate = LocalDate.now(ZoneId.of("Europe/Dublin"));
        assertThat(trigger.getScheduledTime()).isEqualTo(LocalDateTime.of(localDate, LocalTime.of(23, 0, 0)));

        var recurrence = trigger.getRecurrence();
        assertThat(recurrence.getFreq().getValue()).isNull();
        assertThat(recurrence.getInterval()).isNull();
        assertThat(recurrence.getRecurrenceRules()).hasSize(1);
        assertThat(recurrence.getRecurrenceRules().get(0)).isEqualTo("FREQ=DAILY;BYHOUR=23;BYMINUTE=0;BYSECOND=0");
    }
}
