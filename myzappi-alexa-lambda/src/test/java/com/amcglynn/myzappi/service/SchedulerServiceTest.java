package com.amcglynn.myzappi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ActionAfterCompletion;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock
    private SchedulerClient mockSchedulerClient;
    @Captor
    private ArgumentCaptor<CreateScheduleRequest> createScheduleRequestArgumentCaptor;

    @Test
    void schedule() {
        var scheduleTime = LocalDateTime.of(2017, 12, 19, 7, 5, 3, 43534);
        new SchedulerService(mockSchedulerClient, "executionRoleArn", "targetLambdaArn")
                .schedule(scheduleTime, "mockAlexaUserId1234567890", ZoneId.of("Europe/Dublin"));
        verify(mockSchedulerClient).createSchedule(createScheduleRequestArgumentCaptor.capture());

        var sentRequest = createScheduleRequestArgumentCaptor.getValue();
        assertThat(sentRequest).isNotNull();
        assertThat(sentRequest.name()).isEqualTo("reminder-567890");
        assertThat(sentRequest.actionAfterCompletion()).isEqualTo(ActionAfterCompletion.DELETE);
        assertThat(sentRequest.scheduleExpression()).isEqualTo("at(2017-12-19T07:05)");
        assertThat(sentRequest.scheduleExpressionTimezone()).isEqualTo("Europe/Dublin");
        assertThat(sentRequest.target().arn()).isEqualTo("targetLambdaArn");
        assertThat(sentRequest.target().roleArn()).isEqualTo("executionRoleArn");
        assertThat(sentRequest.target().input()).isEqualTo("{\n\"type\": \"reminderUpdate\",\n" +
                "\"alexaBaseUrl\": \"https://api.eu.amazonalexa.com\",\n\"alexaUserId\": \"mockAlexaUserId1234567890\"\n}");
    }
}
