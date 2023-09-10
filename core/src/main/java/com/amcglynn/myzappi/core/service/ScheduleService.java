package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.ScheduleRepository;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.UserId;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class ScheduleService {

    private ScheduleRepository repository;

    public List<Schedule> listSchedules(UserId userId) {
        return repository.read(userId.toString());
    }

    public Schedule createSchedule(UserId userId, Schedule schedule) {
        var schedules = repository.read(userId.toString());
        var scheduleWithId = newSchedule(schedule);
        schedules.add(scheduleWithId);
        repository.write(userId.toString(), schedules);
        return scheduleWithId;
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
