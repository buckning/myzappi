package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.exception.UserNotLoggedInException;
import com.amcglynn.myzappi.core.model.HubCredentials;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyEnergiServiceTest {

    private MyEnergiService myEnergiService;
    @Mock
    private LoginService mockLoginService;
    @Mock
    private UserIdResolver mockUserIdResolver;

    private final String userId = "userId";
    private final SerialNumber zappiSerialNumber = SerialNumber.from("56781234");
    private final SerialNumber serialNumber = SerialNumber.from("12345678");

    @BeforeEach
    void setUp() {
        when(mockLoginService.readCredentials(UserId.from(userId))).thenReturn(Optional.of(new HubCredentials(serialNumber, "myDemoApiKey")));
        when(mockLoginService.readDevices(UserId.from(userId))).thenReturn(List.of(new ZappiDevice(zappiSerialNumber)));
        when(mockUserIdResolver.getUserId()).thenReturn(userId);
    }

    @Test
    void testConstructorThrowsUserNotLoggedInExceptionWhenThereIsNoRowInTheDb() {
        when(mockLoginService.readCredentials(UserId.from(userId))).thenReturn(Optional.empty());
        var throwable = catchThrowable(() -> new MyEnergiService.Builder(mockLoginService).build(mockUserIdResolver));
        assertThat(throwable).isInstanceOf(UserNotLoggedInException.class);
        assertThat(throwable.getMessage()).isEqualTo("User not logged in - userId");
    }

    @Test
    void getEnergyStatus() {
        myEnergiService = new MyEnergiService.Builder(mockLoginService).build(mockUserIdResolver);
        var energyStatus = myEnergiService.getEnergyStatus();
        assertThat(energyStatus.getSolarGenerationKW().getDouble()).isEqualTo(2.723);
        assertThat(energyStatus.getConsumingKW().getDouble()).isEqualTo(2.647);
        assertThat(energyStatus.getImportingKW().getDouble()).isEqualTo(0.0);
        assertThat(energyStatus.getExportingKW().getDouble()).isEqualTo(0.076);
        assertThat(energyStatus).isNotNull();
    }
}
