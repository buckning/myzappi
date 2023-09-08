package com.amcglynn.myzappi.login.rest.controller;

import com.amcglynn.myzappi.login.rest.Request;
import com.amcglynn.myzappi.login.rest.RequestMethod;
import com.amcglynn.myzappi.login.rest.Response;
import com.amcglynn.myzappi.login.rest.ServerException;
import com.amcglynn.myzappi.login.rest.request.ScheduleRequest;
import com.amcglynn.myzappi.login.rest.response.TariffRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScheduleController implements RestController {

    @Override
    public Response handle(Request request) {
        if (request.getMethod() == RequestMethod.POST) {
            postSchedule(request);
            return new Response(200, request.getBody());
        }
        if (request.getMethod() == RequestMethod.GET) {
            return new Response(200, "{}");
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
        } catch (JsonProcessingException e) {
            log.info("Failed to deserialise object", e);
            throw new ServerException(400);
        }
    }
}