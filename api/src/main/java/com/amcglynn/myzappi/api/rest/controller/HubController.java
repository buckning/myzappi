package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.api.CompleteLoginRequest;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the registering and retrieval of the myenergi hub/gateway.
 */
@Slf4j
public class HubController implements RestController {

    private final RegistrationService registrationService;

    public HubController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Override
    public Response handle(Request request) {
        if (request.getMethod() == RequestMethod.POST) {
            return post(request);
        }
        if (request.getMethod() == RequestMethod.GET) {
            return get(request);
        }
        if (request.getMethod() == RequestMethod.DELETE) {
            registrationService.delete(request.getUserId());
            return new Response(204);
        }
        log.info("Unsupported method for hub - {}", request.getMethod());
        throw new ServerException(404);
    }

    private Response post(Request request) {
        if ("/hub/refresh".equals(request.getPath())) {
            return refreshDeploymentDetails(request);
        }
        register(request);
        return new Response(200, request.getBody());
    }

    private Response refreshDeploymentDetails(Request request) {
        registrationService.refreshDeploymentDetails(request.getUserId());
        return new Response(201);
    }

    @SneakyThrows
    private Response get(Request request) {
        if ("/v2/hub".equals(request.getPath())) {
            var body = new ObjectMapper().writeValueAsString(registrationService.readDevices(request.getUserId()));
            return new Response(200, body);
        }
        var creds = registrationService.read(request.getUserId().toString());
        if (creds.isEmpty()) {
            log.info("No hub found for user {}", request.getUserId());
            return new Response(404);
        }
        var body = new ObjectMapper().writeValueAsString(creds.get());
        return new Response(200, body);
    }

    public void register(Request request) {
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
    }
}
