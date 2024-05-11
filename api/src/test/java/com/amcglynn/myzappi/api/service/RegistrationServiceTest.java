package com.amcglynn.myzappi.api.service;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.MyEnergiClientFactory;
import com.amcglynn.myenergi.apiresponse.MyEnergiDeviceStatus;
import com.amcglynn.myenergi.apiresponse.StatusResponse;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.core.dal.DevicesRepository;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.LibbiDevice;
import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import com.amcglynn.myzappi.core.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private LoginService mockLoginService;
    @Mock
    private DevicesRepository mockDevicesRepository;
    @Mock
    private MyEnergiClient mockMyEnergiClient;
    @Mock
    private MyEnergiClientFactory mockMyEnergiClientFactory;
    @Mock
    private StatusResponse mockZappiStatusResponse;
    @Mock
    private StatusResponse mockEddiStatusResponse;
    @Mock
    private StatusResponse mockLibbiStatusResponse;
    @Mock
    private MyEnergiDeviceStatus mockZappiDeviceStatus;
    @Mock
    private MyEnergiDeviceStatus mockEddiDeviceStatus;
    @Mock
    private MyEnergiDeviceStatus mockLibbiDeviceStatus;
    @Captor
    private ArgumentCaptor<List<MyEnergiDevice>> deviceCaptor;
    private RegistrationService service;
    private final String apiKey = "testApiKey";
    private final SerialNumber hubSerialNumber = SerialNumber.from("11223344");
    private final SerialNumber zappiSerialNumber = SerialNumber.from("10000001");
    private final SerialNumber eddiSerialNumber = SerialNumber.from("20000001");
    private final SerialNumber libbiSerialNumber = SerialNumber.from("30000001");
    private final UserId userId = UserId.from("testUserId");

    @BeforeEach
    void setUp() {
        when(mockMyEnergiClientFactory.newMyEnergiClient(hubSerialNumber.toString(), apiKey)).thenReturn(mockMyEnergiClient);
        when(mockZappiDeviceStatus.getSerialNumber()).thenReturn(zappiSerialNumber.toString());
        when(mockEddiDeviceStatus.getSerialNumber()).thenReturn(eddiSerialNumber.toString());
        when(mockLibbiDeviceStatus.getSerialNumber()).thenReturn(libbiSerialNumber.toString());
        when(mockZappiStatusResponse.getZappi()).thenReturn(List.of(mockZappiDeviceStatus));
        when(mockZappiStatusResponse.getEddi()).thenReturn(null);
        when(mockZappiStatusResponse.getLibbi()).thenReturn(null);
        when(mockEddiStatusResponse.getEddi()).thenReturn(List.of(mockEddiDeviceStatus));
        when(mockEddiStatusResponse.getZappi()).thenReturn(null);
        when(mockEddiStatusResponse.getLibbi()).thenReturn(null);
        when(mockLibbiStatusResponse.getLibbi()).thenReturn(List.of(mockLibbiDeviceStatus));
        when(mockLibbiStatusResponse.getZappi()).thenReturn(null);
        when(mockLibbiStatusResponse.getEddi()).thenReturn(null);
        when(mockMyEnergiClient.getStatus()).thenReturn(List.of(mockZappiStatusResponse, mockEddiStatusResponse, mockLibbiStatusResponse));
        service = new RegistrationService(mockLoginService, mockDevicesRepository, mockMyEnergiClientFactory);
    }

    @Test
    void registerThrows409ServerExceptionWhenNoZappiIsFound() {
        when(mockMyEnergiClient.getStatus()).thenReturn(List.of());
        var serverException = catchThrowableOfType(() ->
                service.register(userId, hubSerialNumber, apiKey), ServerException.class);
        assertThat(serverException.getStatus()).isEqualTo(409);
    }

    @Test
    void readDevices() {
        var devices = List.of(
                new ZappiDevice(zappiSerialNumber),
                new EddiDevice(eddiSerialNumber, "tank1", "tank2"));
        when(mockDevicesRepository.read(userId)).thenReturn(devices);
        var response = service.readDevices(userId);
        assertThat(response).isEqualTo(devices);
    }

    @Test
    void getDevice() {
        var zappiDevice = new ZappiDevice(zappiSerialNumber);
        var devices = List.of(
                zappiDevice,
                new EddiDevice(eddiSerialNumber, "tank1", "tank2"));
        when(mockDevicesRepository.read(userId)).thenReturn(devices);
        var response = service.getDevice(userId, zappiSerialNumber);
        assertThat(response).isPresent().contains(zappiDevice);
    }

    @Test
    void getDeviceReturnsEmptyOptionalWhenNotFound() {
        var devices = List.of(
                new ZappiDevice(zappiSerialNumber),
                new EddiDevice(eddiSerialNumber, "tank1", "tank2"));
        when(mockDevicesRepository.read(userId)).thenReturn(devices);
        var response = service.getDevice(userId, SerialNumber.from("12344624"));
        assertThat(response).isEmpty();
    }

    @Test
    void registerWithZappiAndEddiAndLibbi() {
        when(mockEddiDeviceStatus.getTank1Name()).thenReturn("tank1");
        when(mockEddiDeviceStatus.getTank2Name()).thenReturn("tank2");
        when(mockMyEnergiClientFactory.newMyEnergiClient(hubSerialNumber.toString(), apiKey))
                .thenReturn(mockMyEnergiClient);
        service.register(userId, hubSerialNumber, apiKey);
        verify(mockMyEnergiClientFactory).newMyEnergiClient(hubSerialNumber.toString(), apiKey);
        verify(mockMyEnergiClientFactory).newMyEnergiClient(hubSerialNumber.toString(), apiKey);
        verify(mockLoginService).register(eq(userId.toString()), eq(hubSerialNumber),
                eq(apiKey), deviceCaptor.capture());

        assertThat(deviceCaptor.getValue()).hasSize(3);
        assertThat(deviceCaptor.getValue().get(0)).isInstanceOf(ZappiDevice.class);
        assertThat(deviceCaptor.getValue().get(0).getSerialNumber()).isEqualTo(zappiSerialNumber);
        assertThat(deviceCaptor.getValue().get(0).getDeviceClass()).isEqualTo(DeviceClass.ZAPPI);

        assertThat(deviceCaptor.getValue().get(1)).isInstanceOf(EddiDevice.class);
        var eddiDevice = (EddiDevice) deviceCaptor.getValue().get(1);
        assertThat(eddiDevice.getSerialNumber()).isEqualTo(eddiSerialNumber);
        assertThat(eddiDevice.getTank1Name()).isEqualTo("tank1");
        assertThat(eddiDevice.getTank2Name()).isEqualTo("tank2");

        assertThat(deviceCaptor.getValue().get(2)).isInstanceOf(LibbiDevice.class);
        assertThat(deviceCaptor.getValue().get(2).getSerialNumber()).isEqualTo(libbiSerialNumber);
        assertThat(deviceCaptor.getValue().get(2).getDeviceClass()).isEqualTo(DeviceClass.LIBBI);

    }

    @Test
    void registerWithEddi() {
        when(mockEddiDeviceStatus.getTank1Name()).thenReturn("tank1");
        when(mockEddiDeviceStatus.getTank2Name()).thenReturn("tank2");
        when(mockMyEnergiClient.getStatus()).thenReturn(List.of(mockEddiStatusResponse));
        service.register(userId, hubSerialNumber, apiKey);
        verify(mockMyEnergiClientFactory).newMyEnergiClient(hubSerialNumber.toString(), apiKey);
        verify(mockLoginService).register(eq(userId.toString()), eq(hubSerialNumber), eq(apiKey), deviceCaptor.capture());

        assertThat(deviceCaptor.getValue()).hasSize(1);
        assertThat(deviceCaptor.getValue().get(0)).isInstanceOf(EddiDevice.class);
        var eddiDevice = (EddiDevice) deviceCaptor.getValue().get(0);
        assertThat(eddiDevice.getSerialNumber()).isEqualTo(eddiSerialNumber);
        assertThat(eddiDevice.getTank1Name()).isEqualTo("tank1");
        assertThat(eddiDevice.getTank2Name()).isEqualTo("tank2");
    }

    @Test
    void registerWithZappi() {
        when(mockMyEnergiClientFactory.newMyEnergiClient(hubSerialNumber.toString(), apiKey))
                .thenReturn(mockMyEnergiClient);

        when(mockEddiStatusResponse.getEddi()).thenReturn(null);
        when(mockMyEnergiClient.getStatus()).thenReturn(List.of(mockZappiStatusResponse, mockEddiStatusResponse));

        service.register(userId, hubSerialNumber, apiKey);

        verify(mockLoginService).register(eq(userId.toString()), eq(hubSerialNumber), eq(apiKey), deviceCaptor.capture());
        verify(mockMyEnergiClientFactory).newMyEnergiClient(hubSerialNumber.toString(), apiKey);

        assertThat(deviceCaptor.getValue()).hasSize(1);
        assertThat(deviceCaptor.getValue().get(0)).isInstanceOf(ZappiDevice.class);
        assertThat(deviceCaptor.getValue().get(0).getSerialNumber()).isEqualTo(zappiSerialNumber);
        assertThat(deviceCaptor.getValue().get(0).getDeviceClass()).isEqualTo(DeviceClass.ZAPPI);
    }
}
