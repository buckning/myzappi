package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myenergi.MyEnergiClientFactory;
import com.amcglynn.myzappi.api.CompleteLoginRequest;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.response.DeviceDiscoveryResponse;
import com.amcglynn.myzappi.api.rest.response.DeviceResponse;
import com.amcglynn.myzappi.api.rest.response.ListDeviceResponse;
import com.amcglynn.myzappi.api.rest.response.MyEnergiDeviceStatusResponse;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
public class DevicesController implements RestController {

    private final RegistrationService registrationService;
    private final MyEnergiService.Builder myEnergiServiceBuilder;

    public DevicesController(RegistrationService registrationService,
                             MyEnergiService.Builder myEnergiServiceBuilder) {
        this.registrationService = registrationService;
        this.myEnergiServiceBuilder = myEnergiServiceBuilder;
    }

    @Override
    public Response handle(Request request) {
        if (request.getMethod() == RequestMethod.GET) {
            return handleGetRequest(request);
        } else if (request.getMethod() == RequestMethod.DELETE){
            return deleteDevices(request);
        }
        log.info("Unsupported method for devices - {}", request.getMethod());
        throw new ServerException(404);
    }

    private Response handleGetRequest(Request request) {
        if ("/devices".equals(request.getPath())) {
            return listDevices(request);
        } else {
            var deviceId = request.getPath().split("/devices/")[1];
            return getDevice(request.getUserId(), SerialNumber.from(deviceId));
        }
    }

    public Response getDevice(Request request) {
        var deviceId = request.getPath().split("/devices/")[1];
        return getDevice(request.getUserId(), SerialNumber.from(deviceId));
    }

    private Response getDevice(UserId userId, SerialNumber serialNumber) {
        return registrationService.getDevice(userId, serialNumber)
                .map(DeviceResponse::new)
                .map(deviceResponse -> {
                    try {
                        return new Response(200, new ObjectMapper().writeValueAsString(deviceResponse));
                    } catch (Exception e) {
                        log.info("Failed to serialize device response for user {}, serialNumber {}", userId, serialNumber, e);
                        throw new ServerException(500);
                    }
                })
                .orElseThrow(() -> new ServerException(404));
    }

    @SneakyThrows
    public Response listDevices(Request request) {
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
    public Response deleteDevices(Request request) {
        registrationService.delete(request.getUserId());
        return new Response(204);
    }

    public Response discoverDevices(Request request) {
        if (request.getBody() == null) {
            log.info("Null body in POST request");
            throw new ServerException(400);
        }
        try {
            var body = new ObjectMapper().readValue(request.getBody(), new TypeReference<CompleteLoginRequest>() {
            });
            var serialNumber = SerialNumber.from(body.getSerialNumber().replaceAll("\\s", "").toLowerCase());
            var apiKey = body.getApiKey().trim();
            registrationService.register(request.getUserId(), serialNumber, apiKey);
            return new Response(202, new ObjectMapper()
                    .writeValueAsString(DeviceDiscoveryResponse.builder()
                            .serialNumber(serialNumber)
                            .build()));
        } catch (JsonProcessingException e) {
            log.info("Invalid request");
            throw new ServerException(400);
        }
    }

    @SneakyThrows
    public Response getDeviceStatus(Request request) {
        var deviceId = request.getPath().split("/devices/")[1].split("/status")[0];
        var serialNumber = SerialNumber.from(deviceId);
        var device = registrationService.getDevice(request.getUserId(), serialNumber)
                .orElseThrow(() -> new ServerException(404));
        var service = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
        if (DeviceClass.ZAPPI == device.getDeviceClass()) {
            ObjectMapper mapper = new ObjectMapper();

            return new Response(200, mapper
                    .writeValueAsString(new MyEnergiDeviceStatusResponse(service.getZappiService()
                    .get()  // safe to call get() as we have already checked the zappi device is owned by the user
                    .getStatusSummary(serialNumber))));
        } else {
            throw new ServerException(404);
        }
//        return myEnergiServiceBuilder.build().getZappiService()request.getUserId(), SerialNumber.from(deviceId));
    }
}
