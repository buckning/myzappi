package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.request.AutomationActivePatchRequest;
import com.amcglynn.myzappi.api.rest.request.AutomationPriorityRequest;
import com.amcglynn.myzappi.api.rest.response.AutomationResponse;
import com.amcglynn.myzappi.core.exception.AutomationValidationException;
import com.amcglynn.myzappi.core.exception.CapacityReachedException;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.service.automation.AutomationService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AutomationController {

    private final AutomationService service;
    private final ObjectMapper objectMapper;

    public AutomationController(AutomationService service) {
        this.service = service;
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @SneakyThrows
    public Response getOptions(Request request) {
        return new Response(200, objectMapper.writeValueAsString(service.getOptions()));
    }

    @SneakyThrows
    public Response getAutomations(Request request) {
        var body = objectMapper.writeValueAsString(new AutomationResponse(service.listAutomations(request.getUserId())));
        return new Response(200, body);
    }

    public Response createAutomation(Request request) {
        try {
            var automation = objectMapper.readValue(request.getBody(), new TypeReference<Automation>() {
            });
            var created = service.createAutomation(request.getUserId(), automation);
            return new Response(200, objectMapper.writeValueAsString(created));
        } catch (MissingDeviceException e) {
            log.info("User {} requested automation for a missing or invalid target device", request.getUserId());
            throw new ServerException(409);
        } catch (CapacityReachedException e) {
            log.info("User {} has reached the maximum number of automations", request.getUserId());
            throw new ServerException(429);
        } catch (AutomationValidationException | JsonProcessingException e) {
            log.info("Invalid automation create request for user {}", request.getUserId(), e);
            throw new ServerException(400);
        }
    }

    public Response updateAutomation(Request request) {
        try {
            var automationId = request.getPath().split("/automations/")[1];
            var update = objectMapper.readValue(request.getBody(), new TypeReference<AutomationActivePatchRequest>() {
            });
            if (update.getActive() == null) {
                throw new ServerException(400);
            }
            service.setActive(request.getUserId(), automationId, update.getActive());
            return new Response(204);
        } catch (JsonProcessingException e) {
            log.info("Invalid automation update request for user {}", request.getUserId(), e);
            throw new ServerException(400);
        }
    }

    public Response reorderAutomations(Request request) {
        try {
            var reorder = objectMapper.readValue(request.getBody(), new TypeReference<AutomationPriorityRequest>() {
            });
            service.reorder(request.getUserId(), reorder.getAutomationIds());
            return new Response(204);
        } catch (AutomationValidationException | JsonProcessingException e) {
            log.info("Invalid automation reorder request for user {}", request.getUserId(), e);
            throw new ServerException(400);
        }
    }

    public Response deleteAutomation(Request request) {
        var automationId = request.getPath().split("/automations/")[1];
        service.deleteAutomation(request.getUserId(), automationId);
        return new Response(204);
    }
}
