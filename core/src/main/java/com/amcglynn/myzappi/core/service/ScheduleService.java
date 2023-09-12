package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.ScheduleDetailsRepository;
import com.amcglynn.myzappi.core.dal.UserScheduleRepository;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.UserId;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ActionAfterCompletion;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindow;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.Target;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
public class ScheduleService {

    private final UserScheduleRepository repository;
    private final ScheduleDetailsRepository scheduleDetailsRepository;
    private final SchedulerClient schedulerClient;
    private final Target.Builder targetBuilder;

    public ScheduleService(UserScheduleRepository repository,
                           ScheduleDetailsRepository scheduleDetailsRepository,
                           SchedulerClient schedulerClient,
                           String schedulerExecutionRoleArn,
                           String schedulerTargetLambdaArn) {
        this.repository = repository;
        this.scheduleDetailsRepository = scheduleDetailsRepository;
        this.schedulerClient = schedulerClient;

        this.targetBuilder = Target.builder()
                .roleArn(schedulerExecutionRoleArn)
                .arn(schedulerTargetLambdaArn);
    }

    public List<Schedule> listSchedules(UserId userId) {
        return repository.read(userId.toString());
    }

    public Schedule createSchedule(UserId userId, Schedule schedule) {
        var schedules = repository.read(userId.toString());
        var scheduleWithId = newSchedule(schedule);
        schedules.add(scheduleWithId);
        repository.write(userId, schedules);
        scheduleDetailsRepository.write(scheduleWithId.getId(), userId);
        createAwsSchedule(userId, scheduleWithId);
        return scheduleWithId;
    }

    private void createAwsSchedule(UserId userId, Schedule schedule) {
        var request = createRequest(schedule, userId);
        var response = schedulerClient.createSchedule(request);
        log.info("Created schedule with id: {}", response.scheduleArn());
    }

    private CreateScheduleRequest createRequest(Schedule schedule, UserId userId) {
//        var normalised = normalise(localDateTime);  // if there is a millisecond component to the LocalDateTime, aws APIs reject with error
        var normalised = LocalDateTime.of(2023, 9, 14, 10, 0, 0);

        Target lambdaTarget = targetBuilder
                .input("{\n\"type\": \"setChargeMode\",\n\"scheduleId\": \"" + schedule.getId() + "\",\n\"lwaUserId\": \"" + userId  + "\"\n}")
                .build();

        return CreateScheduleRequest.builder()
                .name(schedule.getId())
                .scheduleExpression("at(" + normalised + ")")
                .scheduleExpressionTimezone(schedule.getZoneId().getId())
                .actionAfterCompletion(ActionAfterCompletion.DELETE)
                .target(lambdaTarget)
                .flexibleTimeWindow(FlexibleTimeWindow.builder()
                        .mode(FlexibleTimeWindowMode.OFF)
                        .build())
                .build();
    }

    public Schedule newSchedule(Schedule schedule) {
        return Schedule.builder()
                .id(UUID.randomUUID().toString())
                .startTime(schedule.getStartTime())
                .days(schedule.getDays())
                .type(schedule.getType())
                .zoneId(schedule.getZoneId())
                .action(schedule.getAction())
                .build();
    }
}
