package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.api.CompleteLoginRequest;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.core.service.TariffService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the registering and retrieval of the myenergi hub/gateway.
 */
@Slf4j
public class HubController {

    private final RegistrationService registrationService;
    private final ScheduleService scheduleService;
    private final TariffService tariffService;

    public HubController(RegistrationService registrationService,
                         ScheduleService scheduleService,
                         TariffService tariffService) {
        this.registrationService = registrationService;
        this.scheduleService = scheduleService;
        this.tariffService = tariffService;
    }

    public Response delete(Request request) {
        var userId = UserId.from(request.getUserId().toString());
        deleteSchedules(userId);
        deleteTariffs(userId);
        registrationService.delete(request.getUserId());
        return new Response(204);
    }

    private void deleteSchedules(UserId userId) {
        try {
            var schedules = scheduleService.listSchedules(userId);
            schedules.forEach(schedule ->
                    scheduleService.deleteSchedule(userId, schedule.getId()));
        } catch (Exception e) {
            log.error("Error deleting schedules for user {}", userId, e);
        }
    }

    private void deleteTariffs(UserId userId) {
        try {
            tariffService.delete(userId.toString());
        } catch (Exception e) {
            log.error("Error deleting tariffs for user {}", userId, e);
        }
    }

    public Response refresh(Request request) {
        return refreshDeploymentDetails(request);
    }

    private Response refreshDeploymentDetails(Request request) {
        registrationService.refreshDeploymentDetails(request.getUserId());
        return new Response(201);
    }

    @SneakyThrows
    public Response get(Request request) {
        var body = new ObjectMapper().writeValueAsString(registrationService.readDevices(request.getUserId()));
        return new Response(200, body);
    }

    public Response register(Request request) {
        if (request.getBody() == null) {
            log.info("Null body in POST request");
            throw new ServerException(400);
        }
        try {
            var body = new ObjectMapper().readValue(request.getBody(), new TypeReference<CompleteLoginRequest>() {
            });
            var serialNumber = body.getSerialNumber().replaceAll("\\s", "").toLowerCase();
            var apiKey = body.getApiKey().trim();
            registrationService.register(request.getUserId(), SerialNumber.from(serialNumber), apiKey);
        } catch (JsonProcessingException e) {
            log.info("Invalid request");
            throw new ServerException(400);
        }
        return new Response(200, request.getBody());
    }
}
