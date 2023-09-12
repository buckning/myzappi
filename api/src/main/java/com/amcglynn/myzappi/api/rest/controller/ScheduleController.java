package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.response.ScheduleResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ScheduleController implements RestController {

    private ScheduleService service;

    @Override
    public Response handle(Request request) {
        if (request.getMethod() == RequestMethod.POST) {
            return new Response(200, postSchedule(request));
        }
        if (request.getMethod() == RequestMethod.GET) {
            return getSchedules(request);
        }
        if (request.getMethod() == RequestMethod.DELETE) {
            return new Response(204);
        }
        log.info("Unsupported method for schedule - {}", request.getMethod());
        throw new ServerException(404);
    }

    private String postSchedule(Request request) {
        try {
            var schedulerRequest = new ObjectMapper().readValue(request.getBody(), new TypeReference<Schedule>() {
            });
            log.info("Got scheduler request {}", schedulerRequest);
            var newSchedule = service.createSchedule(request.getUserId(), schedulerRequest);
            return new ObjectMapper().writeValueAsString(newSchedule);
        } catch (JsonProcessingException e) {
            log.info("Failed to deserialise object", e);
            throw new ServerException(400);
        }
    }

    @SneakyThrows
    private Response getSchedules(Request request) {
        var schedules = service.listSchedules(UserId.from(request.getUserId().toString()));
        var body = new ObjectMapper().writeValueAsString(new ScheduleResponse(schedules));
        return new Response(200, body);
    }
}