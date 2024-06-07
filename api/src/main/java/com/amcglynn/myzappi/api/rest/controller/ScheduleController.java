package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.validator.ScheduleValidator;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.response.ScheduleResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
public class ScheduleController {

    private final ScheduleService service;
    private final ObjectMapper objectMapper;
    @Setter(AccessLevel.PACKAGE)
    private ScheduleValidator validator;

    public ScheduleController(ScheduleService service) {
        this.service = service;
        this.validator = new ScheduleValidator();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public Response deleteSchedule(Request request) {
        var resourceId = request.getPath().split("/schedules/")[1];
        service.deleteSchedule(request.getUserId(), resourceId);
        return new Response(204);
    }

    public Response createSchedule(Request request) {
        try {
            var schedulerRequest = objectMapper.readValue(request.getBody(), new TypeReference<Schedule>() {
            });
            log.info("Got scheduler request {}", schedulerRequest);
            validator.validate(schedulerRequest);
            var newSchedule = service.createSchedule(request.getUserId(), schedulerRequest);
            return new Response(200, objectMapper.writeValueAsString(newSchedule));
        } catch (MissingDeviceException e) {
            log.info("User {} requested schedule for device which they don't own", request.getUserId());
            throw new ServerException(409);
        } catch (JsonProcessingException e) {
            log.info("Failed to deserialise object", e);
            throw new ServerException(400);
        }
    }

    @SneakyThrows
    public Response getSchedules(Request request) {
        var targetFilter = request.getQueryStringParameters().get("target");
        var schedules = service.listSchedules(UserId.from(request.getUserId().toString()));
        var filteredSchedules = schedules.stream()
                .filter(target -> isTarget(target, targetFilter))
                .collect(Collectors.toList());
        var body = objectMapper.writeValueAsString(new ScheduleResponse(filteredSchedules));
        return new Response(200, body);
    }

    private boolean isTarget(Schedule schedule, String targetSerialNumber) {
        if (targetSerialNumber == null) {
            return true;
        }

        return schedule.getAction().getTarget()
                .map(targetSerialNumber::equals).orElse(false);
    }
}