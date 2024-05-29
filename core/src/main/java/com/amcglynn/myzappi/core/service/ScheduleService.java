package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.ScheduleDetailsRepository;
import com.amcglynn.myzappi.core.dal.UserScheduleRepository;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleActionType;
import com.amcglynn.myzappi.core.model.ScheduleRecurrence;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ActionAfterCompletion;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindow;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.SchedulerException;
import software.amazon.awssdk.services.scheduler.model.Target;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class ScheduleService {

    private final UserScheduleRepository repository;
    private final ScheduleDetailsRepository scheduleDetailsRepository;
    private final LoginService loginService;
    private final SchedulerClient schedulerClient;
    private final Target.Builder targetBuilder;

    public ScheduleService(UserScheduleRepository repository,
                           ScheduleDetailsRepository scheduleDetailsRepository,
                           SchedulerClient schedulerClient,
                           LoginService loginService,
                           String schedulerExecutionRoleArn,
                           String schedulerTargetLambdaArn) {
        this.repository = repository;
        this.scheduleDetailsRepository = scheduleDetailsRepository;
        this.schedulerClient = schedulerClient;
        this.loginService = loginService;

        this.targetBuilder = Target.builder()
                .roleArn(schedulerExecutionRoleArn)
                .arn(schedulerTargetLambdaArn);
    }

    public Optional<Schedule> getSchedule(String scheduleId) {
        var result = scheduleDetailsRepository.read(scheduleId);

        if (result.isEmpty()) {
            return Optional.empty();
        }

        var schedules = listSchedules(result.get().getLwaUserId());

        return schedules.stream()
                .filter(schedule -> schedule.getId().equals(scheduleId))
                .findFirst();
    }

    public List<Schedule> listSchedules(UserId userId) {
        return repository.read(userId);
    }

    public Schedule createSchedule(UserId userId, Schedule schedule) {
        validate(userId, schedule);
        var schedulesFromDb = repository.read(userId);
        var schedules = new ArrayList<>(schedulesFromDb);
        var scheduleWithId = newSchedule(schedule);
        schedules.add(scheduleWithId);
        repository.write(userId, schedules);
        scheduleDetailsRepository.write(scheduleWithId.getId(), userId);
        createAwsSchedule(userId, scheduleWithId);
        return scheduleWithId;
    }

    private void validate(UserId userId, Schedule schedule) {
        var devices = loginService.readDevices(userId);

        // check that the user owns the target device
        validateUserOwnsTheTargetDevice(userId, devices, schedule);
        validateRequestedDeviceIsTheCorrectType(userId, devices, schedule);
    }

    private void validateUserOwnsTheTargetDevice(UserId userId, List<MyEnergiDevice> devices, Schedule schedule) {
        var target = schedule.getAction().getTarget().orElse(null);
        var scheduleType = schedule.getAction().getType();
        var deviceClassOfSchedule = ScheduleActionType.from(scheduleType).getDeviceClass();
        if (target != null) {
            devices.stream()
                    .filter(d -> d.getSerialNumber().equals(SerialNumber.from(target)))
                    .filter(d -> d.getDeviceClass().equals(deviceClassOfSchedule))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.info("User {} does not own the target device {} or the device class is incorrect. Device class = {}",
                                userId, target, deviceClassOfSchedule);
                        return new MissingDeviceException("Target device class is incorrect or user does not own requested device");
                    });

        }
    }

    private void validateRequestedDeviceIsTheCorrectType(UserId userId, List<MyEnergiDevice> devices, Schedule schedule) {
        var scheduleType = schedule.getAction().getType();
        var deviceClassOfSchedule = ScheduleActionType.from(scheduleType).getDeviceClass();
        devices.stream()
                .filter(device -> device.getDeviceClass().equals(deviceClassOfSchedule))
                .findFirst()
                .orElseThrow(() -> {
                    log.info("User {} does not have the required device for schedule type {}", userId, scheduleType);
                    return new MissingDeviceException("Device not found");
                });
    }

    private void createAwsSchedule(UserId userId, Schedule schedule) {
        CreateScheduleRequest request;
        if (schedule.getRecurrence() == null) {
            request = createRequest(schedule, userId);
        } else {
            request = createRecurringRequest(schedule, userId);
        }

        var response = schedulerClient.createSchedule(request);
        log.info("Created schedule with id: {}", response.scheduleArn());
    }

    private CreateScheduleRequest createRequest(Schedule schedule, UserId userId) {
        var normalised = normalise(schedule.getStartDateTime());  // if there is a millisecond component to the LocalDateTime, aws APIs reject with error

        Target lambdaTarget = targetBuilder
                .input("{\n\"scheduleId\": \"" + schedule.getId() + "\",\n\"lwaUserId\": \"" + userId  + "\"\n}")
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

    private CreateScheduleRequest createRecurringRequest(Schedule schedule, UserId userId) {
        Target lambdaTarget = targetBuilder
                .input("{\n\"scheduleId\": \"" + schedule.getId() + "\",\n\"lwaUserId\": \"" + userId  + "\"\n}")
                .build();

        return CreateScheduleRequest.builder()
                .name(schedule.getId())
                .scheduleExpression("cron(" + buildCronString(schedule.getRecurrence()) + ")")
                .scheduleExpressionTimezone(schedule.getZoneId().getId())
                .actionAfterCompletion(ActionAfterCompletion.NONE)
                .target(lambdaTarget)
                .flexibleTimeWindow(FlexibleTimeWindow.builder()
                        .mode(FlexibleTimeWindowMode.OFF)
                        .build())
                .build();
    }

    private String buildCronString(ScheduleRecurrence recurrence) {
        return recurrence.getTimeOfDay().getMinute() + " " +
                recurrence.getTimeOfDay().getHour() + " ? * " +
                recurrence.getDaysOfWeek().stream()
                        .sorted()
                        .collect(Collectors.toList()).toString().replaceAll("[\\[\\]\\s]", "") + " *";
    }

    private LocalDateTime normalise(LocalDateTime callbackTime) {
        return LocalDateTime.of(callbackTime.getYear(),
                callbackTime.getMonth(),
                callbackTime.getDayOfMonth(),
                callbackTime.getHour(),
                callbackTime.getMinute(),
                0);
    }

    public Schedule newSchedule(Schedule schedule) {
        return Schedule.builder()
                .id(UUID.randomUUID().toString())
                .startDateTime(schedule.getStartDateTime())
                .zoneId(schedule.getZoneId())
                .action(schedule.getAction())
                .recurrence(schedule.getRecurrence())
                .build();
    }

    /**
     * Deletes a schedule from the database but not from AWS. It deletes the schedule from two tables in the database,
     * the user_schedules table and the schedule_details table.
     * This is for schedules with Action after completion set to DELETE.
     * @param scheduleId The id of the schedule to delete
     */
    public void deleteLocalSchedule(String scheduleId) {
        var schedules = scheduleDetailsRepository.read(scheduleId);
        if (schedules.isEmpty()) {
            log.warn("Schedule with id {} not found", scheduleId);
            return;
        }
        var userId = schedules.get().getLwaUserId();
        var schedulesFromDb = repository.read(userId);
        var remainingSchedules = new ArrayList<>(schedulesFromDb);
        remainingSchedules.removeIf(schedule -> schedule.getId().equals(scheduleId));
        repository.update(userId, remainingSchedules);
        scheduleDetailsRepository.delete(scheduleId);
    }

    /**
     * Deletes a schedule from the database and from AWS. It deletes the schedule from two tables in the database,
     * the user_schedules table and the schedule_details table.
     * This is intended to be invoked by the API only when the user wants to delete the schedule. It should not be
     * invoked automatically when the schedule is completed since would unintentionally remove recurring schedules.
     * @param userId The id of the user who owns the schedule
     * @param scheduleId The id of the schedule to delete
     */
    public void deleteSchedule(UserId userId, String scheduleId) {
        var schedules = scheduleDetailsRepository.read(scheduleId);
        if (schedules.isEmpty()) {
            log.warn("Schedule with id {} not found", scheduleId);
            return;
        }
        if (userId.equals(schedules.get().getLwaUserId())) {
            deleteAwsSchedule(scheduleId);
            var schedulesFromDb = repository.read(userId);
            var remainingSchedules = new ArrayList<>(schedulesFromDb);
            remainingSchedules.removeIf(schedule -> schedule.getId().equals(scheduleId));
            repository.update(userId, remainingSchedules);
            scheduleDetailsRepository.delete(scheduleId);
        }
    }

    private void deleteAwsSchedule(String scheduleId) {
        try {
            schedulerClient.deleteSchedule(DeleteScheduleRequest.builder()
                    .name(scheduleId).build());
        } catch (SchedulerException e) {
            log.info("Failed to delete schedule with id: {}", scheduleId);
        }
    }
}
