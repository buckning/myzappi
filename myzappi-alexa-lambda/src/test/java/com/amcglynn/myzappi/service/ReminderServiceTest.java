package com.amcglynn.myzappi.service;

import com.amazon.ask.model.services.reminderManagement.PushNotification;
import com.amazon.ask.model.services.reminderManagement.PushNotificationStatus;
import com.amazon.ask.model.services.reminderManagement.RecurrenceFreq;
import com.amazon.ask.model.services.reminderManagement.ReminderManagementServiceClient;
import com.amazon.ask.model.services.reminderManagement.ReminderRequest;
import com.amazon.ask.model.services.reminderManagement.ReminderResponse;
import com.amazon.ask.model.services.reminderManagement.TriggerType;
import com.amcglynn.lwa.LwaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
    void testCreateRecurringReminder() {
        var response = reminderService.createDailyRecurringReminder(LocalTime.of(23, 0), "Test content",
                Locale.forLanguageTag("en-GB"), ZoneId.of("Europe/Dublin"));

        assertThat(response).isNotNull().isEqualTo("testAlertToken");
        verify(mockReminderClient).createReminder(reminderRequestCaptor.capture());

        var reminderRequest = reminderRequestCaptor.getValue();
        verifyContent(reminderRequest);
        verifyTrigger(reminderRequest);
        assertThat(reminderRequest.getPushNotification())
                .isEqualTo(PushNotification.builder().withStatus(PushNotificationStatus.ENABLED).build());
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
}
