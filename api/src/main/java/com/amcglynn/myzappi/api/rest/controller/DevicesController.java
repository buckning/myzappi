package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.response.DeviceResponse;
import com.amcglynn.myzappi.api.rest.response.ListDeviceResponse;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
public class DevicesController implements RestController {

    private final RegistrationService registrationService;

    public DevicesController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Override
    public Response handle(Request request) {
        if (request.getMethod() == RequestMethod.GET) {
            return listDevices(request);
        } else if (request.getMethod() == RequestMethod.DELETE){
            return deleteDevice(request);
        }
        log.info("Unsupported method for devices - {}", request.getMethod());
        throw new ServerException(404);
    }

    @SneakyThrows
    private Response listDevices(Request request) {
        var typeFilter = request.getQueryStringParameters().get("type");

        var devices = registrationService.readDevices(request.getUserId())
                .stream().map(DeviceResponse::new)
                .filter(device -> filterByType(device, typeFilter))
                .collect(Collectors.toList());
        var body = new ObjectMapper().writeValueAsString(new ListDeviceResponse(devices));
        return new Response(200, body);
    }

    private boolean filterByType(DeviceResponse deviceResponse, String typeFilter) {
        return typeFilter == null || deviceResponse.getType().toString().equals(typeFilter);
    }

    @SneakyThrows
    private Response deleteDevice(Request request) {
        registrationService.delete(request.getUserId());
        return new Response(204);
    }
}
