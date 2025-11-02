package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.LibbiState;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myenergi.units.Watt;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.service.RegistrationService;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.LibbiDevice;
import com.amcglynn.myzappi.core.model.LibbiStatus;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import com.amcglynn.myzappi.core.service.LibbiService;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.StateReconcilerService;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.core.service.EddiService;
import com.amcglynn.myzappi.core.model.EddiStatus;
import com.amcglynn.myenergi.EddiState;
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
import static org.mockito.Mockito.verify;
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
    @Mock
    private LibbiService mockLibbiService;
    @Mock
    private EddiService mockEddiService;
    @Mock
    private StateReconcilerService mockStateReconcilerService;
    private DevicesController controller;
    private UserId userId;
    private SerialNumber libbiSerialNumber;

    @BeforeEach
    void setUp() {
        userId = UserId.from("userId");
        libbiSerialNumber = SerialNumber.from("30000001");
        controller = new DevicesController(mockRegistrationService, mockMyEnergiServiceBuilder, mockStateReconcilerService);
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
        var response = controller.listDevices(new Request(UserId.from("userId"), RequestMethod.GET, "/devices", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent()
                .isEqualTo(Optional.of("""
                        {"devices":[\
                        {"serialNumber":"serial","type":"zappi"},\
                        {"serialNumber":"eddiSerialNumber","type":"eddi"}]\
                        }"""));
    }

    @Test
    void testGetDevice() {
        when(mockRegistrationService.getDevice(UserId.from("userId"), SerialNumber.from("12345678")))
                .thenReturn(Optional.of(new ZappiDevice(SerialNumber.from("serial"))));
        var response = controller.getDevice(new Request(UserId.from("userId"), RequestMethod.GET, "/devices/12345678", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent()
                .isEqualTo(Optional.of("""
                        {"serialNumber":"serial","type":"zappi"}\
                        """));
    }

    @Test
    void testGetDevicesReturns404WhenDeviceNotFound() {
        when(mockRegistrationService.getDevice(userId, SerialNumber.from("12345678")))
                .thenReturn(Optional.empty());
        var exception = catchThrowableOfType(() -> controller.getDevice(new Request(userId,
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
        var response = controller.listDevices(request);
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
        var response = controller.listDevices(new Request(UserId.from("userId"), RequestMethod.GET, "/devices", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent()
                .isEqualTo(Optional.of("{\"devices\":[]}"));
    }

    @Test
    void testDeleteDevice() {
        var response = controller.deleteDevices(new Request(RequestMethod.DELETE, "/devices", "userId"));
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

    @Test
    void testGetLibbiStatus() {
        when(mockRegistrationService.getDevice(UserId.from("userId"), libbiSerialNumber))
                .thenReturn(Optional.of(new LibbiDevice(libbiSerialNumber)));
        when(mockMyEnergiServiceBuilder.build(any()))
                .thenReturn(mockMyEnergiService);
        when(mockMyEnergiService.getLibbiService()).thenReturn(Optional.of(mockLibbiService));
        when(mockLibbiService.getStatus(userId, libbiSerialNumber))
                .thenReturn(LibbiStatus.builder()
                        .serialNumber(libbiSerialNumber)
                        .state(LibbiState.OFF)
                        .stateOfChargePercentage(60)
                        .energyTargetKWh(new KiloWattHour(5.520))
                        .chargeFromGridEnabled(true)
                        .batterySizeKWh(new KiloWattHour(10.200))
                        .energyTargetPercentage(20)
                        .build());

        var response = controller.getDeviceStatus(new Request(UserId.from("userId"), RequestMethod.GET, "/devices/30000001/status", null));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("""
                {\
                "serialNumber":"30000001",\
                "state":"OFF",\
                "stateDescription":"Off",\
                "stateOfChargePercentage":60,\
                "batterySizeKWh":"10.2",\
                "chargeFromGridEnabled":true,\
                "energyTargetKWh":"5.5",\
                "energyTargetPercentage":20}\
                """));
    }

    @Test
    void testGetDeviceStatusForEddi() {
        var eddiSerial = SerialNumber.from("20000001");
        var eddiDevice = new EddiDevice(eddiSerial, "Tank 1", "Tank 2");
        var eddiStatus = EddiStatus.builder()
                .serialNumber(eddiSerial)
                .state(EddiState.DIVERTING)
                .activeHeater("Tank 1")
                .gridImport(new Watt(300L))
                .gridExport(new Watt(0L))
                .generated(new Watt(1500L))
                .consumed(new Watt(1200L))
                .tank1Name("Tank 1")
                .tank2Name("Tank 2")
                .consumedThisSessionKWh(new KiloWattHour(3.5))
                .diversionAmountWatts(new Watt(300L))
                .build();

        when(mockRegistrationService.getDevice(userId, eddiSerial)).thenReturn(Optional.of(eddiDevice));
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        when(mockMyEnergiService.getEddiService()).thenReturn(Optional.of(mockEddiService));
        when(mockEddiService.getStatus(eddiSerial)).thenReturn(eddiStatus);

        var request = new Request(userId, RequestMethod.GET, "/devices/20000001/status", null);
        var response = controller.getDeviceStatus(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isPresent();

        // Verify all important fields are present in the response
        String responseBody = response.getBody().get();
        assertThat(responseBody).contains("\"serialNumber\":\"20000001\"");
        assertThat(responseBody).contains("\"state\":\"DIVERTING\"");
        assertThat(responseBody).contains("\"activeHeater\":\"Tank 1\"");
        assertThat(responseBody).contains("\"importingKW\":\"0.3\"");
        assertThat(responseBody).contains("\"exportingKW\":\"0.0\"");
        assertThat(responseBody).contains("\"solarGenerationKW\":\"1.5\"");
        assertThat(responseBody).contains("\"consumingKW\":\"1.2\"");
        assertThat(responseBody).contains("\"consumedThisSessionKWh\":\"3.5\"");
        assertThat(responseBody).contains("\"diversionAmountKW\":\"0.3\"");
        assertThat(responseBody).contains("\"tank1Name\":\"Tank 1\"");
        assertThat(responseBody).contains("\"tank2Name\":\"Tank 2\"");
    }

    @Test
    void setLibbiTargetEnergy() {
        var body = """
                {"targetEnergyWh":5500}
                """;
        when(mockRegistrationService.getDevice(UserId.from("userId"), libbiSerialNumber))
                .thenReturn(Optional.of(new LibbiDevice(libbiSerialNumber)));
        when(mockMyEnergiServiceBuilder.build(any()))
                .thenReturn(mockMyEnergiService);
        when(mockMyEnergiService.getLibbiService()).thenReturn(Optional.of(mockLibbiService));

        var response = controller.setLibbiTargetEnergy(new Request(UserId.from("userId"), RequestMethod.POST, "/devices/30000001/target-energy", body));
        assertThat(response.getStatus()).isEqualTo(202);
        verify(mockLibbiService).setChargeTarget(userId, libbiSerialNumber, 5500);
    }

    @Test
    void setLibbiTargetEnergyReturns404WhenDeviceClassIsNotLibbi() {
        var body = """
                {"targetEnergyWh":5500}
                """;
        when(mockRegistrationService.getDevice(UserId.from("userId"), libbiSerialNumber))
                .thenReturn(Optional.of(new ZappiDevice(libbiSerialNumber)));
        when(mockMyEnergiServiceBuilder.build(any()))
                .thenReturn(mockMyEnergiService);

        var exception = catchThrowableOfType(() ->
                controller.setLibbiTargetEnergy(new Request(UserId.from("userId"), RequestMethod.POST, "/devices/30000001/target-energy", body)), ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(404);
    }

    @Test
    void setLibbiTargetEnergyReturns400WhenNoBodyIsInTheRequest() {
        var exception = catchThrowableOfType(() ->
                controller.setLibbiTargetEnergy(new Request(UserId.from("userId"), RequestMethod.POST, "/devices/30000001/target-energy", null)), ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(400);
    }

    @Test
    void setLibbiTargetEnergyReturns400WhenBodyIsInvalid() {
        var invalidBody = """
                {"ta1rgetEnergyWh":5500}
                """;
        var exception = catchThrowableOfType(() ->
                controller.setLibbiTargetEnergy(new Request(UserId.from("userId"), RequestMethod.POST, "/devices/30000001/target-energy", invalidBody)), ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(400);
    }

    @Test
    void setLibbiChargeFromGrid() {
        var body = """
                {"chargeFromGrid":true}
                """;
        when(mockRegistrationService.getDevice(UserId.from("userId"), libbiSerialNumber))
                .thenReturn(Optional.of(new LibbiDevice(libbiSerialNumber)));
        when(mockMyEnergiServiceBuilder.build(any()))
                .thenReturn(mockMyEnergiService);
        when(mockMyEnergiService.getLibbiService()).thenReturn(Optional.of(mockLibbiService));

        var response = controller.setLibbiChargeFromGrid(new Request(UserId.from("userId"), RequestMethod.POST, "/devices/30000001/charge-from-grid", body));
        assertThat(response.getStatus()).isEqualTo(202);
        verify(mockLibbiService).setChargeFromGrid(userId, libbiSerialNumber, true);
    }

    @Test
    void setLibbiChargeFromGridReturns404WhenDeviceClassIsNotLibbi() {
        var body = """
                {"chargeFromGrid":true}
                """;
        when(mockRegistrationService.getDevice(UserId.from("userId"), libbiSerialNumber))
                .thenReturn(Optional.of(new ZappiDevice(libbiSerialNumber)));
        when(mockMyEnergiServiceBuilder.build(any()))
                .thenReturn(mockMyEnergiService);

        var exception = catchThrowableOfType(() ->
                controller.setLibbiChargeFromGrid(new Request(UserId.from("userId"), RequestMethod.POST, "/devices/30000001/charge-from-grid", body)), ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(404);
    }

    @Test
    void setLibbiChargeFromGridReturns400WhenNoBodyIsInTheRequest() {
        var exception = catchThrowableOfType(() ->
                controller.setLibbiChargeFromGrid(new Request(UserId.from("userId"), RequestMethod.POST, "/devices/30000001/charge-from-grid", null)), ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(400);
    }

    @Test
    void setLibbiChargeFromGridReturns400WhenBodyIsInvalid() {
        var invalidBody = """
                {"targetEnergyWh":5500}
                """;
        var exception = catchThrowableOfType(() ->
                controller.setLibbiChargeFromGrid(new Request(UserId.from("userId"), RequestMethod.POST, "/devices/30000001/charge-from-grid", invalidBody)), ServerException.class);
        assertThat(exception.getStatus()).isEqualTo(400);
    }
}
