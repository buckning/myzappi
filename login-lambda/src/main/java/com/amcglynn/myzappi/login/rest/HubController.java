package com.amcglynn.myzappi.login.rest;

import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.login.CompleteLoginRequest;
import com.amcglynn.myzappi.login.service.RegistrationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles the registering and retrieval of the myenergi hub/gateway.
 */
public class HubController implements RestController {

    private final RegistrationService registrationService;

    public HubController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Override
    public Response handle(Request request) {
        if (request.getMethod() == RequestMethod.POST) {
            register(request);

            return new Response(200, request.getBody());
        }
        if (request.getMethod() == RequestMethod.DELETE) {
            registrationService.delete(request.getUserId());
            return new Response(204);
        }
        System.out.println("Unsupported method for hub - " + request.getMethod());
        throw new ServerException(404);
    }

    public void register(Request request) {
        if (request.getBody() == null) {
            System.err.println("Null body in POST request");
            throw new ServerException(400);
        }
        try {
            var body = new ObjectMapper().readValue(request.getBody(), new TypeReference<CompleteLoginRequest>() {
            });
            var serialNumber = body.getSerialNumber().replaceAll("\\s", "").toLowerCase();
            var apiKey = body.getApiKey().trim();
            registrationService.register(request.getUserId(), SerialNumber.from(serialNumber), apiKey);
        } catch (JsonProcessingException e) {
            System.err.println("Invalid request");
            throw new ServerException(400);
        }
    }
}
