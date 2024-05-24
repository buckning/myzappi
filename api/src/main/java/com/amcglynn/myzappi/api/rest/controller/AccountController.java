package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.request.RegisterMyEnergiAccountRequest;
import com.amcglynn.myzappi.api.rest.response.RegisterAccountResponse;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.amcglynn.myzappi.core.model.EmailAddress;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the registering and retrieval of the myenergi account information.
 */
@Slf4j
public class AccountController {

    private final RegistrationService registrationService;
    private final ObjectMapper objectMapper;

    public AccountController(RegistrationService registrationService) {
        this.registrationService = registrationService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Register a myenergi myaccount email and password with the myzappi account. This is needed for oauth authentication
     * with myenergi APIs for libbi control and others.
     * @param request request containing myenergi myaccount email and password
     * @return
     */
    public Response register(Request request) {
        if (request.getBody() == null) {
            log.info("Null body in POST request");
            throw new ServerException(400);
        }
        try {
            var body = new ObjectMapper().readValue(request.getBody(), new TypeReference<RegisterMyEnergiAccountRequest>() {
            });
            var email = body.getEmail().trim().toLowerCase();
            var password = body.getPassword().trim();
            registrationService.register(request.getUserId(), EmailAddress.from(email), password);
            return new Response(200, objectMapper.writeValueAsString(RegisterAccountResponse.builder().email(email).build()));
        } catch (JsonProcessingException e) {
            log.info("Invalid request");
            throw new ServerException(400);
        }
    }
}
