package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.CompleteLoginRequest;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.request.LibbiModeMapper;
import com.amcglynn.myzappi.api.rest.request.SetLibbiChargeFromGridRequest;
import com.amcglynn.myzappi.api.rest.request.SetLibbiTargetEnergyRequest;
import com.amcglynn.myzappi.api.rest.request.SetModeRequest;
import com.amcglynn.myzappi.api.rest.request.ZappiChargeModeMapper;
import com.amcglynn.myzappi.api.rest.response.DeviceDiscoveryResponse;
import com.amcglynn.myzappi.api.rest.response.DeviceResponse;
import com.amcglynn.myzappi.api.rest.response.ListDeviceResponse;
import com.amcglynn.myzappi.api.rest.response.MyEnergiEddiStatusResponse;
import com.amcglynn.myzappi.api.rest.response.MyEnergiDeviceStatusResponse;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.amcglynn.myzappi.core.model.Action;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.StateReconcilerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
public class DevicesController {

    private final RegistrationService registrationService;
    private final MyEnergiService.Builder myEnergiServiceBuilder;
    private final StateReconcilerService stateReconcilerService;

    public DevicesController(RegistrationService registrationService,
            MyEnergiService.Builder myEnergiServiceBuilder,
            StateReconcilerService stateReconcilerService) {
        this.registrationService = registrationService;
        this.myEnergiServiceBuilder = myEnergiServiceBuilder;
        this.stateReconcilerService = stateReconcilerService;
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

    public Response setLibbiTargetEnergy(Request request) {
        if (request.getBody() == null) {
            log.info("Null body in set libbi target energy request");
            throw new ServerException(400);
        }
        try {
            var body = new ObjectMapper().readValue(request.getBody(), new TypeReference<SetLibbiTargetEnergyRequest>() {
            });
            var deviceId = request.getPath().split("/devices/")[1].split("/target-energy")[0];
            var serialNumber = SerialNumber.from(deviceId);
            var device = registrationService.getDevice(request.getUserId(), serialNumber)
                    .orElseThrow(() -> {
                        log.info("Device not found");
                        return new ServerException(404);
                    });
            var service = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
            if (DeviceClass.LIBBI == device.getDeviceClass()) {
                log.info("Setting target energy to {} for device {}", body.getTargetEnergyWh(), serialNumber);
                service.getLibbiService().get().setChargeTarget(request.getUserId(), serialNumber, body.getTargetEnergyWh());
                return new Response(202);
            } else {
                log.info("Device is not a libbi");
                throw new ServerException(404);
            }
        } catch (JsonProcessingException e) {
            log.info("Invalid request");
            throw new ServerException(400);
        }
    }

    public Response setLibbiChargeFromGrid(Request request) {
        if (request.getBody() == null) {
            log.info("Null body in set libbi charge from grid request");
            throw new ServerException(400);
        }
        try {
            var body = new ObjectMapper().readValue(request.getBody(), new TypeReference<SetLibbiChargeFromGridRequest>() {
            });
            var deviceId = request.getPath().split("/devices/")[1].split("/charge-from-grid")[0];
            var serialNumber = SerialNumber.from(deviceId);
            var device = registrationService.getDevice(request.getUserId(), serialNumber)
                    .orElseThrow(() -> {
                        log.info("Device not found");
                        return new ServerException(404);
                    });
            var service = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
            if (DeviceClass.LIBBI == device.getDeviceClass()) {
                log.info("Setting charge-from-grid to {} for device {}", body.isChargeFromGrid(), serialNumber);
                service.getLibbiService().get().setChargeFromGrid(request.getUserId(), serialNumber, body.isChargeFromGrid());
                return new Response(202);
            } else {
                log.info("Device is not a libbi");
                throw new ServerException(404);
            }
        } catch (JsonProcessingException e) {
            log.info("Invalid request");
            throw new ServerException(400);
        }
    }

    public Response setMode(Request request) {
        if (request.getBody() == null) {
            log.info("Null body in set mode request");
            throw new ServerException(400);
        }
        try {
            var body = new ObjectMapper().readValue(request.getBody(), new TypeReference<SetModeRequest>() {
            });
            var deviceId = request.getPath().split("/devices/")[1].split("/mode")[0];
            var serialNumber = SerialNumber.from(deviceId);
            var device = registrationService.getDevice(request.getUserId(), serialNumber)
                    .orElseThrow(() -> new ServerException(404));
            var service = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
            if (DeviceClass.ZAPPI == device.getDeviceClass()) {
                var response = handleSetZappiMode(body, service, serialNumber);
                try {
                    stateReconcilerService.pushReconcileRequest(
                            request.getUserId(),
                            new Action(
                                    "setChargeMode",
                                    new ZappiChargeModeMapper().getZappiChargeMode(body.getMode().toLowerCase()).get().toString(),
                                    serialNumber,
                                    DeviceClass.ZAPPI));
                } catch (Exception e) {
                    log.error("Failed setting reconcile request for user {}, device {}", request.getUserId(), serialNumber, e);
                }
                return response;
            } else if (DeviceClass.LIBBI == device.getDeviceClass()) {
                return handleSetLibbiMode(body, service, serialNumber);
            } else {
                throw new ServerException(404);
            }
        } catch (JsonProcessingException e) {
            log.info("Invalid request");
            throw new ServerException(400);
        }
    }

    private Response handleSetZappiMode(SetModeRequest body, MyEnergiService service, SerialNumber serialNumber) {
        var zappiMode = new ZappiChargeModeMapper().getZappiChargeMode(body.getMode().toLowerCase());
        if (zappiMode.isEmpty()) {
            log.info("Invalid zappi mode requested");
            throw new ServerException(400);
        }
        service.getZappiService().get().setChargeMode(serialNumber, zappiMode.get());
        return new Response(202);
    }

    private Response handleSetLibbiMode(SetModeRequest body, MyEnergiService service, SerialNumber serialNumber) {
        var libbiMode = new LibbiModeMapper().getLibbiMode(body.getMode().toLowerCase());
        if (libbiMode.isEmpty()) {
            log.info("Invalid libbi mode requested");
            throw new ServerException(400);
        }
        service.getLibbiService().get().setMode(serialNumber, libbiMode.get());
        return new Response(202);
    }

    @SneakyThrows
    public Response getDeviceStatus(Request request) {
        var mapper = new ObjectMapper();
        var deviceId = request.getPath().split("/devices/")[1].split("/status")[0];
        var serialNumber = SerialNumber.from(deviceId);
        var device = registrationService.getDevice(request.getUserId(), serialNumber)
                .orElseThrow(() -> new ServerException(404));
        var service = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
        if (DeviceClass.ZAPPI == device.getDeviceClass()) {
            return new Response(200, mapper
                    .writeValueAsString(new MyEnergiDeviceStatusResponse(service.getZappiService()
                    .get()  // safe to call get() as we have already checked the zappi device is owned by the user
                    .getStatusSummary(serialNumber))));
        } else if (DeviceClass.LIBBI == device.getDeviceClass()) {
            return new Response(200, mapper.writeValueAsString(service.getLibbiService()
                    .get()
                    .getStatus(request.getUserId(), serialNumber)));
        } else if (DeviceClass.EDDI == device.getDeviceClass()) {
            var status = service.getEddiService()
                    .get()
                    .getStatus(serialNumber);
            return new Response(200, mapper.writeValueAsString(
                    new MyEnergiEddiStatusResponse(status)
            ));
        } else {
            throw new ServerException(404);
        }
    }
}
