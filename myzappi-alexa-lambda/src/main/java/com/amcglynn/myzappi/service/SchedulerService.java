package com.amcglynn.myzappi.service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ActionAfterCompletion;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindow;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.Target;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
public class SchedulerService {

    private final SchedulerClient schedulerClient;
    private final Target.Builder targetBuilder;

    public SchedulerService(SchedulerClient schedulerClient, String schedulerExecutionRoleArn, String schedulerTargetLambdaArn) {
        this.schedulerClient = schedulerClient;
        this.targetBuilder = Target.builder()
                .roleArn(schedulerExecutionRoleArn)
                .arn(schedulerTargetLambdaArn);
    }

    public void schedule(LocalDateTime callbackTime, String alexaUserId, ZoneId zoneId) {
        var createScheduleRequest = createRequest(callbackTime, alexaUserId, zoneId);
        log.info("Creating scheduled job with request {}", createScheduleRequest);
        var response = schedulerClient.createSchedule(createScheduleRequest);
        log.info("Created scheduled job {}", response);
    }

    private CreateScheduleRequest createRequest(LocalDateTime localDateTime, String alexaUserId, ZoneId zoneId) {
        var normalised = normalise(localDateTime);  // if there is a millisecond component to the LocalDateTime, aws APIs reject with error

        Target lambdaTarget = targetBuilder
                .input("{\n\"type\": \"reminderUpdate\",\n\"alexaBaseUrl\": \"https://api.eu.amazonalexa.com\",\n\"alexaUserId\": \"" + alexaUserId  + "\"\n}")
                .build();

        return CreateScheduleRequest.builder()
                .name("reminder-" + alexaUserId.substring(alexaUserId.length() - 6))
                .scheduleExpression("at(" + normalised + ")")
                .scheduleExpressionTimezone(zoneId.getId())
                .actionAfterCompletion(ActionAfterCompletion.DELETE)
                .target(lambdaTarget)
                .flexibleTimeWindow(FlexibleTimeWindow.builder()
                        .mode(FlexibleTimeWindowMode.OFF)
                        .build())
                .build();
    }

    private LocalDateTime normalise(LocalDateTime callbackTime) {
        return LocalDateTime.of(callbackTime.getYear(),
                callbackTime.getMonth(),
                callbackTime.getDayOfMonth(),
                callbackTime.getHour(),
                callbackTime.getMinute(),
                0);
    }
}
