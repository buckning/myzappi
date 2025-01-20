package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.MyEnergiClientFactory;
import com.amcglynn.myenergi.MyEnergiOAuthClient;
import com.amcglynn.myenergi.apiresponse.LibbiChargeSetupResponse;
import com.amcglynn.myenergi.apiresponse.LibbiStatus;
import com.amcglynn.myenergi.apiresponse.LibbiStatusResponse;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.model.MyEnergiAccountCredentials;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class LibbiServiceTest {

    @Mock
    private LoginService mockLoginService;
    @Mock
    private MyEnergiClient mockMyEnergiClient;
    @Mock
    private MyEnergiClientFactory mockMyEnergiClientFactory;
    @Mock
    private MyEnergiOAuthClient mockMyEnergiOAuthClient;
    private LibbiService service;
    private final UserId userId = UserId.from("testUser");

    @BeforeEach
    void setUp() {
        when(mockMyEnergiClientFactory.newMyEnergiOAuthClient(anyString(), anyString()))
                .thenReturn(mockMyEnergiOAuthClient);
        when(mockMyEnergiOAuthClient.getLibbiChargeSetup("30000001"))
                .thenReturn(LibbiChargeSetupResponse.builder()
                        .energyTarget(5520)
                        .chargeFromGrid(true).build());

        var libbiStatusResponse = new LibbiStatusResponse();
        libbiStatusResponse.setLibbi(List.of(LibbiStatus.builder()
                .serialNumber("30000001")
                .status(1)
                .stateOfCharge(60)
                .batterySizeWh(10200)
                .build()));
        when(mockMyEnergiClient.getLibbiStatus("30000001"))
                .thenReturn(libbiStatusResponse);

        service = new LibbiService(mockMyEnergiClient, mockMyEnergiClientFactory, mockLoginService, List.of(SerialNumber.from("30000001")));
    }

    @Test
    void getStatusIgnoresOAuthClientInfoWhenMyAccountInformationIsNotInDb() {
        when(mockLoginService.readMyEnergiAccountCredentials(userId)).thenReturn(Optional.empty());
        var response = service.getStatus(userId, SerialNumber.from("30000001"));
        assertThat(response).isNotNull();
        assertThat(response.getSerialNumber()).isEqualTo(SerialNumber.from("30000001"));
        assertThat(response.getChargeFromGridEnabled()).isNull();
        assertThat(response.getEnergyTargetKWh()).isNull();
        assertThat(response.getBatterySizeKWh()).isEqualTo(new KiloWattHour(10.2));
        assertThat(response.getStateOfChargePercentage()).isEqualTo(60);
    }

    @Test
    void getStatusReturnsLibbiStatusAndDropsChargeSetupFieldsInResponseWhenChargeSetupApiCallThrowsAnError() {
        when(mockMyEnergiClientFactory.newMyEnergiOAuthClient(anyString(), anyString()))
                .thenThrow(ClientException.class);

        when(mockLoginService.readMyEnergiAccountCredentials(userId))
                .thenReturn(Optional.of(new MyEnergiAccountCredentials(userId.toString(), "user@test.com", "password")));
        var response = service.getStatus(userId, SerialNumber.from("30000001"));
        assertThat(response).isNotNull();
        assertThat(response.getSerialNumber()).isEqualTo(SerialNumber.from("30000001"));

        assertThat(response.getChargeFromGridEnabled()).isNull();
        assertThat(response.getEnergyTargetKWh()).isNull();
        assertThat(response.getBatterySizeKWh()).isEqualTo(new KiloWattHour(10.2));
        assertThat(response.getStateOfChargePercentage()).isEqualTo(60);
    }

    @Test
    void getStatusUsesOAuthClientInfoWhenMyAccountInformationIsInDb() {
        when(mockLoginService.readMyEnergiAccountCredentials(userId))
                .thenReturn(Optional.of(new MyEnergiAccountCredentials(userId.toString(), "user@test.com", "password")));
        var response = service.getStatus(userId, SerialNumber.from("30000001"));
        assertThat(response).isNotNull();
        assertThat(response.getSerialNumber()).isEqualTo(SerialNumber.from("30000001"));

        assertThat(response.getChargeFromGridEnabled()).isTrue();
        assertThat(response.getEnergyTargetKWh()).isEqualTo(new KiloWattHour(5.520));
        assertThat(response.getBatterySizeKWh()).isEqualTo(new KiloWattHour(10.2));
        assertThat(response.getStateOfChargePercentage()).isEqualTo(60);
    }

    @Test
    void setChargeTarget() {
        when(mockLoginService.readMyEnergiAccountCredentials(userId))
                .thenReturn(Optional.of(new MyEnergiAccountCredentials(userId.toString(), "user@test.com", "password")));
        service.setChargeTarget(userId, SerialNumber.from("30000001"), 100);
        verify(mockMyEnergiOAuthClient).setTargetEnergy("30000001", 9200);
    }

    @MethodSource("chargeTargetPercentForLibbiWith2Batteries")
    @ParameterizedTest
    void setChargeTarget(int percent, int expectedEnergy) {
        when(mockLoginService.readMyEnergiAccountCredentials(userId))
                .thenReturn(Optional.of(new MyEnergiAccountCredentials(userId.toString(), "user@test.com", "password")));
        service.setChargeTarget(userId, SerialNumber.from("30000001"), percent);
        verify(mockMyEnergiOAuthClient).setTargetEnergy("30000001", expectedEnergy);
    }

    @Test
    void getUsableEnergy() {
        var libbiStatusResponse = new LibbiStatusResponse();
        libbiStatusResponse.setLibbi(List.of(LibbiStatus.builder()
                .serialNumber("30000001")
                .status(1)
                .stateOfCharge(60)
                .batterySizeWh(5120)
                .build()));
        when(mockMyEnergiClient.getLibbiStatus("30000001"))
                .thenReturn(libbiStatusResponse);
        assertThat(service.getUsableEnergy(SerialNumber.from("30000001")).getDouble()).isEqualTo(4.6);
    }

    @Test
    void getUsableEnergyForBatterySizeOf2() {
        // 2 libbis has mbc of 10200
        assertThat(service.getUsableEnergy(SerialNumber.from("30000001")).getDouble()).isEqualTo(9.2);
    }

    @Test
    void getUsableEnergyForBatterySizeOf3() {
        var libbiStatusResponse = new LibbiStatusResponse();
        libbiStatusResponse.setLibbi(List.of(LibbiStatus.builder()
                .serialNumber("30000001")
                .status(1)
                .stateOfCharge(60)
                .batterySizeWh(15360)
                .build()));
        when(mockMyEnergiClient.getLibbiStatus("30000001"))
                .thenReturn(libbiStatusResponse);
        assertThat(service.getUsableEnergy(SerialNumber.from("30000001")).getDouble()).isEqualTo(13.8);
    }

    @Test
    void getUsableEnergyForBatterySizeOf4() {
        var libbiStatusResponse = new LibbiStatusResponse();
        libbiStatusResponse.setLibbi(List.of(LibbiStatus.builder()
                .serialNumber("30000001")
                .status(1)
                .stateOfCharge(60)
                .batterySizeWh(20480)
                .build()));
        when(mockMyEnergiClient.getLibbiStatus("30000001"))
                .thenReturn(libbiStatusResponse);
        assertThat(service.getUsableEnergy(SerialNumber.from("30000001")).getDouble()).isEqualTo(18.4);
    }

    @Test
    void setChargeTargetTo50Percent() {
        when(mockLoginService.readMyEnergiAccountCredentials(userId))
                .thenReturn(Optional.of(new MyEnergiAccountCredentials(userId.toString(), "user@test.com", "password")));
        service.setChargeTarget(userId, SerialNumber.from("30000001"), 50);
        verify(mockMyEnergiOAuthClient).setTargetEnergy("30000001", 4600);
    }

    @Test
    void setChargeTargetIsIgnoredIfTheUserDoesNotHaveMyEnergiAccountCredentialsConfigured() {
        when(mockLoginService.readMyEnergiAccountCredentials(userId))
                .thenReturn(Optional.empty());
        service.setChargeTarget(userId, SerialNumber.from("30000001"), 5520);
        verify(mockMyEnergiOAuthClient, never()).setTargetEnergy(anyString(), anyInt());
    }

    @Test
    void setChargeFromGrid() {
        when(mockLoginService.readMyEnergiAccountCredentials(userId))
                .thenReturn(Optional.of(new MyEnergiAccountCredentials(userId.toString(), "user@test.com", "password")));
        service.setChargeFromGrid(userId, SerialNumber.from("30000001"), true);
        verify(mockMyEnergiOAuthClient).setChargeFromGrid("30000001", true);
    }

    @Test
    void setChargeFromGridIsIgnoredIfTheUserDoesNotHaveMyEnergiAccountCredentialsConfigured() {
        when(mockLoginService.readMyEnergiAccountCredentials(userId))
                .thenReturn(Optional.empty());
        service.setChargeFromGrid(userId, SerialNumber.from("30000001"), true);
        verify(mockMyEnergiOAuthClient, never()).setChargeFromGrid(anyString(), anyBoolean());
    }

    private static Stream<Arguments> chargeTargetPercentForLibbiWith2Batteries() {
        return Stream.of(
                Arguments.of(5, 460),
                Arguments.of(20, 1840),
                Arguments.of(50, 4600),
                Arguments.of(90, 8280),
                Arguments.of(95, 8740),
                Arguments.of(100, 9200)
        );
    }
}
