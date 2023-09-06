package com.amcglynn.myzappi.service;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.services.ServiceClientFactory;
import com.amazon.ask.model.services.reminderManagement.ReminderManagementServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceFactoryTest {

    @Mock
    private HandlerInput mockHandlerInput;
    @Mock
    private ServiceClientFactory mockServiceClientFactory;
    @Mock
    private SchedulerService mockSchedulerService;


    @BeforeEach
    void setUp() {
        when(mockHandlerInput.getServiceClientFactory()).thenReturn(mockServiceClientFactory);
        when(mockServiceClientFactory.getReminderManagementService()).thenReturn(mock(ReminderManagementServiceClient.class));

        new Clock().localDateTime(ZoneId.of("Europe/Dublin"));  // added for test coverage. Clock is a simple wrapper object so no value in unit tests
        new Clock().localDate(ZoneId.of("Europe/Dublin"));
    }

    @Test
    void testNewReminderService() {
        var factory = new ReminderServiceFactory(mockSchedulerService);
        var instance = factory.newReminderService(mockHandlerInput);
        assertThat(instance).isNotNull().isInstanceOf(ReminderService.class);
    }
}
