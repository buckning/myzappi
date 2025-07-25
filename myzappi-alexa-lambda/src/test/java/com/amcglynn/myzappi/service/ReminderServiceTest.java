package com.amcglynn.myzappi.service;

import com.amazon.ask.model.services.reminderManagement.PushNotification;
import com.amazon.ask.model.services.reminderManagement.PushNotificationStatus;
import com.amazon.ask.model.services.reminderManagement.ReminderManagementServiceClient;
import com.amazon.ask.model.services.reminderManagement.ReminderRequest;
import com.amazon.ask.model.services.reminderManagement.ReminderResponse;
import com.amazon.ask.model.services.reminderManagement.TriggerType;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.lwa.Reminder;
import com.amcglynn.lwa.Reminders;
import com.amcglynn.myzappi.core.service.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    @Mock
    private SchedulerService mockSchedulerService;
    @Mock
    private Clock mockClock;

    private LocalDateTime reminderDateTime = LocalDateTime.of(2023, 5, 19, 23, 0, 0);

    @Captor
    private ArgumentCaptor<ReminderRequest> reminderRequestCaptor;

    @BeforeEach
    void setUp() {
        when(mockClock.localDateTime(ZoneId.of("Europe/Dublin"))).thenReturn(reminderDateTime);
        when(mockClock.localDate(ZoneId.of("Europe/Dublin"))).thenReturn(reminderDateTime.toLocalDate());
        when(mockReminderClient.createReminder(any())).thenReturn(ReminderResponse.builder().withAlertToken("testAlertToken").build());
        when(mockReminderClient.updateReminder(any(), any())).thenReturn(ReminderResponse.builder().withAlertToken("testAlertToken").build());
        this.reminderService = new ReminderService(mockReminderClient, mockLwaClient, mockSchedulerService, mockClock);
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
        verifyTrigger(reminderRequest);
        assertThat(reminderRequest.getPushNotification())
                .isEqualTo(PushNotification.builder().withStatus(PushNotificationStatus.ENABLED).build());
    }

    @Test
    void testHandleReminderMessageDoesNotPerformAnyUpdatesWhenThereAreNoRemindersConfigured() {
        when(mockLwaClient.getReminders(anyString(), anyString())).thenReturn(new Reminders(0, List.of()));

        reminderService.handleReminderMessage("testAccessToken", "", "", () -> true);

        verify(mockLwaClient).getReminders("https://api.eu.amazonalexa.com", "testAccessToken");
        verify(mockReminderClient, never()).updateReminder(any(), any());
    }

    @Test
    void testHandleReminderMessageDelaysTheReminderBy24HoursWhenTheTestConditionIsMetAndCreatesScheduledCallback() {
        final var testCondition = true;
        when(mockLwaClient.getReminders(anyString(), anyString())).thenReturn(getReminders(reminderDateTime));

        reminderService.handleReminderMessage("testAccessToken", "mockAlexaId", "Europe/Dublin", () -> testCondition);

        verify(mockLwaClient).getReminders("https://api.eu.amazonalexa.com", "testAccessToken");
        verify(mockReminderClient).updateReminder(any(), reminderRequestCaptor.capture());
        var reminderUpdateRequest = reminderRequestCaptor.getValue();
        assertThat(reminderUpdateRequest).isNotNull();
        verifyTrigger(reminderUpdateRequest, reminderDateTime.plusDays(1));

        // schedule new SQS alert for tomorrow 5 minutes before the reminder
        verify(mockSchedulerService).schedule(reminderDateTime.plusDays(1).minusMinutes(5), "mockAlexaId", ZoneId.of("Europe/Dublin"));
    }

    @Test
    void testHandleReminderMessageDelaysTheReminderBy24HoursWhenScheduledTimeIsInThePast() {
        final var testCondition = true;
        when(mockLwaClient.getReminders(anyString(), anyString())).thenReturn(getReminders(reminderDateTime.minusDays(3)));

        reminderService.handleReminderMessage("testAccessToken", "mockAlexaId", "Europe/Dublin", () -> testCondition);

        verify(mockLwaClient).getReminders("https://api.eu.amazonalexa.com", "testAccessToken");
        verify(mockReminderClient).updateReminder(any(), reminderRequestCaptor.capture());
        var reminderUpdateRequest = reminderRequestCaptor.getValue();
        assertThat(reminderUpdateRequest).isNotNull();
        verifyTrigger(reminderUpdateRequest, reminderDateTime.plusDays(1));

        // schedule new SQS alert for tomorrow 5 minutes before the reminder
        verify(mockSchedulerService).schedule(reminderDateTime.plusDays(1).minusMinutes(5), "mockAlexaId", ZoneId.of("Europe/Dublin"));
    }

    @Test
    void testHandleReminderMessageDoesNotDelayTheReminderWhenTheTestConditionIsNotMet() {
        final var testCondition = false;
        when(mockLwaClient.getReminders(anyString(), anyString())).thenReturn(getReminders(reminderDateTime));

        reminderService.handleReminderMessage("testAccessToken", "mockAlexaId", "Europe/Dublin", () -> testCondition);

        verify(mockLwaClient).getReminders("https://api.eu.amazonalexa.com", "testAccessToken");
        verify(mockReminderClient, never()).updateReminder(any(), any());
        // schedule new SQS alert for tomorrow 5 minutes before the reminder
        verify(mockSchedulerService).schedule(reminderDateTime.plusDays(1).minusMinutes(5), "mockAlexaId", ZoneId.of("Europe/Dublin"));
    }

    @Test
    void testHandleReminderMessageDoesNotDelayTheReminderWhenReminderIsInTheFutureAndSchedulesCallback5MinutesBeforeReminder() {
        when(mockLwaClient.getReminders(anyString(), anyString())).thenReturn(getReminders(reminderDateTime.plusHours(7)));

        reminderService.handleReminderMessage("testAccessToken", "mockAlexaId", "Europe/Dublin", () -> true);

        verify(mockLwaClient).getReminders("https://api.eu.amazonalexa.com", "testAccessToken");
        verify(mockReminderClient, never()).updateReminder(any(), any());

        // schedule the reminder today 5 minutes before the reminder
        verify(mockSchedulerService).schedule(reminderDateTime.plusHours(7).minusMinutes(5), "mockAlexaId", ZoneId.of("Europe/Dublin"));
    }

    private Reminders getReminders() {
        return new Reminders(1, List.of(Reminder.builder()
                .alertToken("testAlertToken")
                .createdTime("2023-08-27T07:58:30.990Z")
                .updatedTime("2023-08-27T07:58:31.211Z")
                .trigger(Reminder.Trigger.builder()
                        .type("SCHEDULED_ABSOLUTE")
                        .scheduledTime("2023-08-28T23:00:00.000")
                        .timeZoneId("Europe/Dublin")
                        .offsetInSeconds(0)
                        .recurrence(Reminder.Recurrence.builder()
                                .freq("DAILY")
                                .startDateTime("2023-08-28T23:00:00.000+01:00")
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

    private Reminders getReminders(LocalDateTime localDateTime) {
        return new Reminders(1, List.of(Reminder.builder()
                .alertToken("testAlertToken")
                .createdTime("2023-08-27T07:58:30.990Z")
                .updatedTime("2023-08-27T07:58:31.211Z")
                .trigger(Reminder.Trigger.builder()
                        .type("SCHEDULED_ABSOLUTE")
                        .scheduledTime(localDateTime.toString())
                        .timeZoneId("Europe/Dublin")
                        .offsetInSeconds(0)
                        .recurrence(Reminder.Recurrence.builder()
                                .freq("DAILY")
                                .startDateTime(ZonedDateTime.of(localDateTime, ZoneId.of("Europe/Dublin")).toString())
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
        verifyTrigger(reminderRequest, LocalDateTime.of(2023, 5, 19, 23, 0, 0));
    }

    private void verifyTrigger(ReminderRequest reminderRequest, LocalDateTime reminderStartDateTime) {
        var trigger = reminderRequest.getTrigger();
        assertThat(trigger).isNotNull();
        assertThat(trigger.getTimeZoneId()).isEqualTo("Europe/Dublin");
        assertThat(trigger.getType()).isEqualTo(TriggerType.SCHEDULED_ABSOLUTE);
        assertThat(trigger.getRecurrence().getStartDateTime())
                .isEqualTo(reminderStartDateTime.minusMinutes(1));
        assertThat(trigger.getScheduledTime()).isNull();

        var recurrence = trigger.getRecurrence();
        assertThat(recurrence.getRecurrenceRules()).containsExactly("FREQ=DAILY;BYHOUR=" +
                reminderStartDateTime.getHour() + ";BYMINUTE=" +
                reminderStartDateTime.getMinute() + ";BYSECOND=" +
                reminderStartDateTime.getSecond());
    }
}
