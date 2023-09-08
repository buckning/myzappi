package com.amcglynn.myzappi.login.rest.controller;

import com.amcglynn.myzappi.core.dal.ScheduleRepository;
import com.amcglynn.myzappi.login.rest.Request;
import com.amcglynn.myzappi.login.rest.RequestMethod;
import com.amcglynn.myzappi.login.rest.Response;
import com.amcglynn.myzappi.login.rest.ServerException;
import com.amcglynn.myzappi.login.rest.request.ScheduleRequest;
import com.amcglynn.myzappi.login.rest.response.ScheduleResponse;
import com.amcglynn.myzappi.login.rest.response.TariffRequest;
import com.amcglynn.myzappi.login.rest.response.TariffResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ScheduleController implements RestController {

    private ScheduleRepository repository;

    @Override
    public Response handle(Request request) {
        if (request.getMethod() == RequestMethod.POST) {
            postSchedule(request);
            return new Response(200, request.getBody());
        }
        if (request.getMethod() == RequestMethod.GET) {
            return getSchedules(request);
        }
        if (request.getMethod() == RequestMethod.DELETE) {
            return new Response(204);
        }
        log.info("Unsupported method for schedule - " + request.getMethod());
        throw new ServerException(404);
    }

    private void postSchedule(Request request) {
        try {
            var schedulerRequest = new ObjectMapper().readValue(request.getBody(), new TypeReference<ScheduleRequest>() {
            });

            log.info("Got scheduler request {}", schedulerRequest);
            repository.write(request.getUserId().toString(), schedulerRequest.getSchedules());
        } catch (JsonProcessingException e) {
            log.info("Failed to deserialise object", e);
            throw new ServerException(400);
        }
    }

    @SneakyThrows
    private Response getSchedules(Request request) {
        var schedules = repository.read(request.getUserId().toString());

        var body = new ObjectMapper().writeValueAsString(new ScheduleResponse(schedules));
        return new Response(200, body);
    }
}