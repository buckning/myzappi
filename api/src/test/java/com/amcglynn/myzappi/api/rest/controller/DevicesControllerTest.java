package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevicesControllerTest {
    @Mock
    private RegistrationService mockRegistrationService;
    @Mock
    private MyEnergiService.Builder mockMyEnergiServiceBuilder;
    @Mock
    private MyEnergiService mockMyEnergiService;
    @Mock
    private ZappiService mockZappiService;
    private DevicesController controller;

    @BeforeEach
    void setUp() {
        controller = new DevicesController(mockRegistrationService, mockMyEnergiServiceBuilder);
    }

    @Test
    void discoverDevices() {
        var body = """
                {"serialNumber":"12345678","apiKey":"myApiKey"}
                """;
        var response = controller.discoverDevices(new Request(RequestMethod.POST, "/devices/discover", body));
        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(response.getBody()).isEqualTo(Optional.of("{\"serialNumber\":\"12345678\"}"));
    }

    @Test
    void discoverDevicesReturns400WhenBodyIsNull() {
        var exception = catchThrowableOfType(() -> controller.discoverDevices(
                new Request(RequestMethod.POST, "/devices/discover", null)), ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(400);
    }

    @Test
    void discoverDevicesReturns400WhenBodyIsMalformed() {
        var body = """
                {"serialNumber":
                """;
        var exception = catchThrowableOfType(() -> controller.discoverDevices(
                new Request(RequestMethod.POST, "/devices/discover", body)), ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(400);
    }

    @Test
    void testListDevices() {
        when(mockRegistrationService.readDevices(UserId.from("userId")))
                .thenReturn(List.of(new ZappiDevice(SerialNumber.from("serial")),
                        new EddiDevice(SerialNumber.from("eddiSerialNumber"), "tank1", "tank2")));
        var response = controller.handle(new Request(UserId.from("userId"), RequestMethod.GET, "/devices", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent()
                .isEqualTo(Optional.of("""
                        {"devices":[\
                        {"serialNumber":"serial","type":"zappi"},\
                        {"serialNumber":"eddiSerialNumber","type":"eddi"}]\
                        }"""));
    }

    @Test
    void testGetDevices() {
        when(mockRegistrationService.getDevice(UserId.from("userId"), SerialNumber.from("12345678")))
                .thenReturn(Optional.of(new ZappiDevice(SerialNumber.from("serial"))));
        var response = controller.handle(new Request(UserId.from("userId"), RequestMethod.GET, "/devices/12345678", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent()
                .isEqualTo(Optional.of("""
                        {"serialNumber":"serial","type":"zappi"}\
                        """));
    }

    @Test
    void testGetDevicesReturns404WhenDeviceNotFound() {
        when(mockRegistrationService.getDevice(UserId.from("userId"), SerialNumber.from("12345678")))
                .thenReturn(Optional.empty());
        var exception = catchThrowableOfType(() -> controller.handle(new Request(UserId.from("userId"),
                RequestMethod.GET, "/devices/12345678", null)), ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(404);
    }

    @Test
    void testListDevicesWithFilter() {
        when(mockRegistrationService.readDevices(UserId.from("userId")))
                .thenReturn(List.of(new ZappiDevice(SerialNumber.from("serial")),
                        new EddiDevice(SerialNumber.from("eddiSerialNumber1"), "tank1", "tank2"),
                        new EddiDevice(SerialNumber.from("eddiSerialNumber2"), "tank1", "tank2")));
        var request = new Request(RequestMethod.GET, "/devices", null, Map.of(), Map.of("type", "eddi"));
        request.setUserId("userId");
        var response = controller.handle(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent()
                .isEqualTo(Optional.of("""
                        {"devices":[\
                        {"serialNumber":"eddiSerialNumber1","type":"eddi"},\
                        {"serialNumber":"eddiSerialNumber2","type":"eddi"}]\
                        }"""));
    }

    @Test
    void testListDevicesNoDevices() {
        when(mockRegistrationService.readDevices(UserId.from("userId")))
                .thenReturn(List.of());
        var response = controller.handle(new Request(UserId.from("userId"), RequestMethod.GET, "/devices", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent()
                .isEqualTo(Optional.of("{\"devices\":[]}"));
    }

    @Test
    void testDeleteDevice() {
        var response = controller.handle(new Request(RequestMethod.DELETE, "/devices", "userId"));
        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    void testGetZappiStatus() {
        when(mockRegistrationService.getDevice(UserId.from("userId"), SerialNumber.from("12345678")))
                .thenReturn(Optional.of(new ZappiDevice(SerialNumber.from("12345678"))));
        when(mockMyEnergiServiceBuilder.build(any()))
                .thenReturn(mockMyEnergiService);
        when(mockMyEnergiService.getZappiService()).thenReturn(Optional.of(mockZappiService));
        when(mockZappiService.getStatusSummary(SerialNumber.from("12345678"))).thenReturn(new ZappiStatusSummary(
                new ZappiStatus("12345678", 1500L, 1400L,
                        24.3, 1000L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.DIVERTING.ordinal(), EvConnectionStatus.CHARGING.getCode(), 23, "v1.2.3")));
        var response = controller.getDeviceStatus(new Request(UserId.from("userId"), RequestMethod.GET, "/devices/12345678/status", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("""
                {"serialNumber":"12345678","type":"zappi","firmware":"v1.2.3",\
                "energy":{"solarGenerationKW":"1.5","consumingKW":"2.5","importingKW":"1.0","exportingKW":"0.0"},\
                "mode":"Eco+","chargeAddedKwh":"24.3","connectionStatus":"CHARGING","chargeStatus":"DIVERTING",\
                "chargeRateKw":"1.4","lockStatus":"CHARGE_ALLOWED"}\
                """));
    }
}
