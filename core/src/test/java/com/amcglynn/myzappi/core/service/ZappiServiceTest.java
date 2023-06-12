package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.apiresponse.ZappiDayHistory;
import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.exception.UserNotLoggedInException;
import com.amcglynn.myzappi.core.model.LoginCode;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.ZappiCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZappiServiceTest {

    private ZappiService zappiService;
    @Mock
    private LoginService mockLoginService;
    @Mock
    private EncryptionService mockEncryptionService;
    @Mock
    private MyEnergiClient mockClient;

    private final String userId = "userId";
    private final SerialNumber serialNumber = SerialNumber.from("12345678");
    private final LoginCode loginCode = LoginCode.from("abc123");
    private final ByteBuffer encryptedApiKey = ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03 });
    private ZappiCredentials zappiCreds;

    @BeforeEach
    void setUp() {
        zappiCreds = new ZappiCredentials(userId, serialNumber, loginCode, encryptedApiKey);
        when(mockLoginService.readCredentials(userId)).thenReturn(Optional.of(zappiCreds));
        when(mockLoginService.isLoggedIn(zappiCreds)).thenReturn(true);
        when(mockEncryptionService.decrypt(encryptedApiKey)).thenReturn("myApiKey");
        this.zappiService = new ZappiService.Builder(mockLoginService, mockEncryptionService).build(userId);
        zappiService.setClient(mockClient);
    }

    @Test
    void testConstructorThrowsUserNotLoggedInExceptionWhenThereIsNoRowInTheDb() {
        when(mockLoginService.readCredentials(userId)).thenReturn(Optional.empty());
        var throwable = catchThrowable(() -> new ZappiService
                .Builder(mockLoginService, mockEncryptionService).build(userId));
        assertThat(throwable).isInstanceOf(UserNotLoggedInException.class);
        assertThat(throwable.getMessage()).isEqualTo("User not logged in - userId");
    }

    @Test
    void testConstructorThrowsUserNotLoggedInExceptionWhenEncryptedApiIsNotConfiguredInDb() {
        zappiCreds = new ZappiCredentials(userId, serialNumber, loginCode);
        when(mockLoginService.readCredentials(userId)).thenReturn(Optional.of(zappiCreds));
        var throwable = catchThrowable(() -> new ZappiService
                .Builder(mockLoginService, mockEncryptionService).build(userId));
        assertThat(throwable).isInstanceOf(UserNotLoggedInException.class);
        assertThat(throwable.getMessage()).isEqualTo("User not logged in - userId");
    }

    @ParameterizedTest
    @MethodSource("boostWithDurationSource")
    void testBoostWithDurationWillBoostToTheNearest15Minutes(String startTime, Duration chargeDuration, String expectedEndTime) {
        Supplier<LocalTime> localTimeSupplier = () -> LocalTime.parse(startTime);
        zappiService.setLocalTimeSupplier(localTimeSupplier);
        var boostStopTime = zappiService.startSmartBoost(chargeDuration);

        var timeFormatterForUrl = DateTimeFormatter.ofPattern("HHmm");
        assertThat(expectedEndTime).isEqualTo(boostStopTime.format(timeFormatterForUrl));
    }

    @ParameterizedTest
    @MethodSource("boostWithEndTime")
    void testBoostUntilTimeWillBoostToTheNearest15Minutes(String endTime, String expectedEndTime) {
        zappiService.setLocalTimeSupplier(LocalTime::now);
        var boostStopTime = zappiService .startSmartBoost(LocalTime.parse(endTime));

        var timeFormatterForUrl = DateTimeFormatter.ofPattern("HHmm");
        assertThat(expectedEndTime).isEqualTo(boostStopTime.format(timeFormatterForUrl));
    }

    @Test
    void testStopBoostGetsProxiedToClient() {
        zappiService.stopBoost();
        verify(mockClient).stopBoost();
    }

    @Test
    void testGetEnergyUsageTransformsClientResponseToZappiDaySummary() {
        var mockHistory = mock(ZappiDayHistory.class);
        when(mockHistory.getReadings()).thenReturn(List.of(new ZappiHistory(2023, 1, 2, 9, 0,
                "Monday", 3600000L, 1800000L,
                360000L, 720000L, 7200000L)));
        var localDate = LocalDate.of(2023, 1, 20);
        when(mockClient.getZappiHistory(localDate))
                .thenReturn(mockHistory);
        var history = zappiService.getEnergyUsage(localDate);
        assertThat(history.getSampleSize()).isOne();
        assertThat(history.getSolarGeneration()).isEqualTo(new KiloWattHour(1));
        assertThat(history.getImported()).isEqualTo(new KiloWattHour(2));
        assertThat(history.getExported()).isEqualTo(new KiloWattHour(0.5));
        assertThat(history.getConsumed()).isEqualTo(new KiloWattHour(2.5));
        assertThat(history.getEvSummary().getBoost()).isEqualTo(new KiloWattHour(0.1));
        assertThat(history.getEvSummary().getDiverted()).isEqualTo(new KiloWattHour(0.2));
        assertThat(history.getEvSummary().getTotal()).isEqualTo(new KiloWattHour(0.3));

        verify(mockClient).getZappiHistory(localDate);
    }

    @Test
    void testSetChargeModeGetsProxiedToClient() {
        zappiService.setChargeMode(ZappiChargeMode.FAST);
        verify(mockClient).setZappiChargeMode(ZappiChargeMode.FAST);
    }

    @Test
    void testStartBoostGetsProxiedToClient() {
        zappiService.startBoost(new KiloWattHour(100));
        verify(mockClient).boost(new KiloWattHour(100));
    }

    private static Stream<Arguments> boostWithDurationSource() {
        return Stream.of(
                Arguments.of("11:32:30", Duration.of(1, ChronoUnit.HOURS), "1230"),
                Arguments.of("23:32:30", Duration.of(1, ChronoUnit.HOURS), "0030"),
                Arguments.of("23:40:30", Duration.of(1, ChronoUnit.HOURS), "0045"),
                Arguments.of("23:44:30", Duration.of(1, ChronoUnit.HOURS), "0045"),
                Arguments.of("09:30:30", Duration.of(1, ChronoUnit.HOURS), "1030"),
                Arguments.of("01:00:59", Duration.of(1, ChronoUnit.HOURS), "0200"),
                // round up
                Arguments.of("01:08:59", Duration.of(1, ChronoUnit.HOURS), "0215"),
                Arguments.of("01:23:59", Duration.of(1, ChronoUnit.HOURS), "0230"),
                Arguments.of("01:38:59", Duration.of(1, ChronoUnit.HOURS), "0245"),
                Arguments.of("01:53:59", Duration.of(1, ChronoUnit.HOURS), "0300"),
                // round down
                Arguments.of("01:07:59", Duration.of(1, ChronoUnit.HOURS), "0200"),
                Arguments.of("01:22:59", Duration.of(1, ChronoUnit.HOURS), "0215"),
                Arguments.of("01:37:59", Duration.of(1, ChronoUnit.HOURS), "0230"),
                Arguments.of("01:52:59", Duration.of(1, ChronoUnit.HOURS), "0245"));
    }

    private static Stream<Arguments> boostWithEndTime() {
        return Stream.of(
                Arguments.of("12:32:30", "1230"),
                Arguments.of("00:32:30", "0030"),
                Arguments.of("00:40:30", "0045"),
                Arguments.of("00:44:30", "0045"),
                Arguments.of("10:30:30", "1030"),
                Arguments.of("02:00:59", "0200"),
                // round up
                Arguments.of("02:08:59", "0215"),
                Arguments.of("02:23:59", "0230"),
                Arguments.of("02:38:59", "0245"),
                Arguments.of("03:53:59", "0400"),
                // round down
                Arguments.of("02:07:59", "0200"),
                Arguments.of("02:22:59", "0215"),
                Arguments.of("02:37:59", "0230"),
                Arguments.of("02:52:59", "0245"));
    }
}