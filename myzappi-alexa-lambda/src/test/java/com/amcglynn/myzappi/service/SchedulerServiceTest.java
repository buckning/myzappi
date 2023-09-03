package com.amcglynn.myzappi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

import java.time.LocalDateTime;
import java.time.ZoneId;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock
    private SchedulerClient mockSchedulerClient;

    @Test
    void name() {
        new SchedulerService(mockSchedulerClient, "executionRoleArn", "targetLambdaArn")
                .schedule(LocalDateTime.now(), "mockAlexaUserId", ZoneId.of("Europe/Dublin"));
    }
}
