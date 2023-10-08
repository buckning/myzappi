package com.amcglynn.myzappi.api.service;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.MyEnergiClientFactory;
import com.amcglynn.myenergi.apiresponse.MyEnergiDeviceStatus;
import com.amcglynn.myenergi.apiresponse.StatusResponse;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private LoginService mockLoginService;
    @Mock
    private MyEnergiClient mockMyEnergiClient;
    @Mock
    private MyEnergiClientFactory mockMyEnergiClientFactory;
    @Mock
    private StatusResponse mockZappiStatusResponse;
    @Mock
    private StatusResponse mockEddiStatusResponse;
    @Mock
    private MyEnergiDeviceStatus mockZappiDeviceStatus;
    @Mock
    private MyEnergiDeviceStatus mockEddiDeviceStatus;
    private RegistrationService service;
    private final String apiKey = "testApiKey";
    private final SerialNumber hubSerialNumber = SerialNumber.from("12345678");
    private final SerialNumber zappiSerialNumber = SerialNumber.from("56781234");
    private final SerialNumber eddiSerialNumber = SerialNumber.from("09876543");
    private final UserId userId = UserId.from("testUserId");

    @BeforeEach
    void setUp() {
        when(mockMyEnergiClientFactory.newMyEnergiClient(hubSerialNumber.toString(), apiKey)).thenReturn(mockMyEnergiClient);
        when(mockZappiDeviceStatus.getSerialNumber()).thenReturn(zappiSerialNumber.toString());
        when(mockEddiDeviceStatus.getSerialNumber()).thenReturn(eddiSerialNumber.toString());
        when(mockZappiStatusResponse.getZappi()).thenReturn(List.of(mockZappiDeviceStatus));
        when(mockZappiStatusResponse.getEddi()).thenReturn(null);
        when(mockEddiStatusResponse.getEddi()).thenReturn(List.of(mockEddiDeviceStatus));
        when(mockEddiStatusResponse.getZappi()).thenReturn(null);
        when(mockMyEnergiClient.getStatus()).thenReturn(List.of(mockZappiStatusResponse, mockEddiStatusResponse));
        service = new RegistrationService(mockLoginService, mockMyEnergiClientFactory);
    }

    @Test
    void registerThrows409ServerExceptionWhenNoZappiIsFound() {
        when(mockMyEnergiClient.getStatus()).thenReturn(List.of(mockEddiStatusResponse));
        var serverException = catchThrowableOfType(() ->
                service.register(userId, hubSerialNumber, apiKey), ServerException.class);
        assertThat(serverException.getStatus()).isEqualTo(409);
    }

    @Test
    void registerWithZappiAndEddi() {
        when(mockMyEnergiClientFactory.newMyEnergiClient(hubSerialNumber.toString(), zappiSerialNumber.toString(), eddiSerialNumber.toString(), apiKey))
                .thenReturn(mockMyEnergiClient);
        service.register(userId, hubSerialNumber, apiKey);
        verify(mockMyEnergiClientFactory).newMyEnergiClient(hubSerialNumber.toString(), apiKey);
        verify(mockMyEnergiClientFactory).newMyEnergiClient(hubSerialNumber.toString(), zappiSerialNumber.toString(),
                eddiSerialNumber.toString(), apiKey);
        verify(mockLoginService).register(userId.toString(), zappiSerialNumber, hubSerialNumber, eddiSerialNumber, apiKey);
    }

    @Test
    void registerWithZappi() {
        when(mockMyEnergiClientFactory.newMyEnergiClient(hubSerialNumber.toString(), zappiSerialNumber.toString(), null, apiKey))
                .thenReturn(mockMyEnergiClient);

        when(mockEddiStatusResponse.getEddi()).thenReturn(null);
        when(mockMyEnergiClient.getStatus()).thenReturn(List.of(mockZappiStatusResponse, mockEddiStatusResponse));

        service.register(userId, hubSerialNumber, apiKey);

        verify(mockLoginService).register(userId.toString(), zappiSerialNumber, hubSerialNumber, null, apiKey);
        verify(mockMyEnergiClientFactory).newMyEnergiClient(hubSerialNumber.toString(), apiKey);
        verify(mockMyEnergiClientFactory).newMyEnergiClient(hubSerialNumber.toString(), zappiSerialNumber.toString(),
                null, apiKey);
    }

    @Test
    void registerTestAccount() {
        when(mockMyEnergiClientFactory.newMyEnergiClient(zappiSerialNumber.toString(), hubSerialNumber.toString(), null, apiKey))
                .thenReturn(mockMyEnergiClient);

        when(mockEddiStatusResponse.getEddi()).thenReturn(List.of());
        when(mockEddiStatusResponse.getZappi()).thenReturn(List.of());
        when(mockMyEnergiClient.getStatus()).thenReturn(List.of(mockZappiStatusResponse, mockEddiStatusResponse));

        service.register(userId, SerialNumber.from("12345678"), "myDemoApiKey");

        verify(mockLoginService).register(userId.toString(), SerialNumber.from("12345678"),
                SerialNumber.from("12345678"), eddiSerialNumber, "myDemoApiKey");
        verify(mockMyEnergiClientFactory, never()).newMyEnergiClient(anyString(), anyString());
        verify(mockMyEnergiClientFactory, never()).newMyEnergiClient(any(),
                anyString(), anyString(), anyString());
    }
}
