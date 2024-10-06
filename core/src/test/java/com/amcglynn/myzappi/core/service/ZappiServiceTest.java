package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.apiresponse.ZappiDayHistory;
import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myenergi.apiresponse.ZappiStatusResponse;
import com.amcglynn.myenergi.units.KiloWattHour;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ZappiServiceTest {

    private ZappiService zappiService;
    @Mock
    private MyEnergiClient mockClient;
    @Mock
    private ZappiStatusResponse mockStatusResponse;
    @Mock
    private ZappiStatus mockZappiStatus;

    @BeforeEach
    void setUp() {
        when(mockZappiStatus.getPhase()).thenReturn(1);
        when(mockZappiStatus.getChargeAddedThisSessionKwh()).thenReturn(5.0);
        when(mockStatusResponse.getZappi()).thenReturn(List.of(mockZappiStatus));
        this.zappiService = new ZappiService(mockClient);
    }

    @ParameterizedTest
    @MethodSource("boostWithDurationSource")
    void testBoostWithDurationWillBoostToTheNearest15Minutes(String startTime, Duration chargeDuration,
                                                             String expectedEndTime, KiloWattHour expectedKiloWattHourSentToCharger) {
        when(mockClient.getZappiStatus()).thenReturn(mockStatusResponse);
        Supplier<LocalTime> localTimeSupplier = () -> LocalTime.parse(startTime);
        zappiService.setLocalTimeSupplier(localTimeSupplier);
        var boostStopTime = zappiService.startSmartBoost(chargeDuration);

        var timeFormatterForUrl = DateTimeFormatter.ofPattern("HHmm");

        verify(mockClient).boost(LocalTime.parse(expectedEndTime, timeFormatterForUrl), expectedKiloWattHourSentToCharger);
        assertThat(expectedEndTime).isEqualTo(boostStopTime.format(timeFormatterForUrl));
    }

    @ParameterizedTest
    @MethodSource("boostWithEndTime")
    void testBoostUntilTimeWillBoostToTheNearest15Minutes(String endTime, String expectedEndTime, KiloWattHour expectedKiloWattHourSentToCharger) {
        when(mockClient.getZappiStatus()).thenReturn(mockStatusResponse);
        zappiService.setLocalTimeSupplier(() -> LocalTime.of(21, 0));
        var boostStopTime = zappiService.startSmartBoost(LocalTime.parse(endTime));

        var timeFormatterForUrl = DateTimeFormatter.ofPattern("HHmm");

        verify(mockClient).boost(LocalTime.parse(expectedEndTime, timeFormatterForUrl), expectedKiloWattHourSentToCharger);

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
    void testGetEnergyUsageAdjustsToLocalTimeForTimeZoneWithNoOffset() {
        var mockHistory = mock(ZappiDayHistory.class);
        when(mockHistory.getReadings()).thenReturn(List.of(new ZappiHistory(2023, 1, 2, 9, 0,
                "Monday", 3600000L, 1800000L,
                360000L, 720000L, 7200000L)));
        var localDate = LocalDate.of(2023, 1, 20);
        when(mockClient.getZappiHistory(localDate, 0))
                .thenReturn(mockHistory);
        zappiService.getEnergyUsage(localDate, ZoneId.of("Europe/Dublin"));

        verify(mockClient).getZappiHistory(localDate, 0);
    }

    @Test
    void testGetEnergyUsageAdjustsToLocalTimeForTimeZoneWithNoOffsetButInDayLightSavingsTime() {
        var mockHistory = mock(ZappiDayHistory.class);
        when(mockHistory.getReadings()).thenReturn(List.of(new ZappiHistory(2023, 1, 2, 9, 0,
                "Monday", 3600000L, 1800000L,
                360000L, 720000L, 7200000L)));
        var localDate = LocalDate.of(2023, 5, 20);
        when(mockClient.getZappiHistory(localDate.minusDays(1), 23))
                .thenReturn(mockHistory);
        zappiService.getEnergyUsage(localDate, ZoneId.of("Europe/Dublin"));

        verify(mockClient).getZappiHistory(localDate.minusDays(1), 23);
    }

    @Test
    void testGetEnergyUsageAdjustsToLocalTimeForTimeZoneWithPositiveOffset() {
        var mockHistory = mock(ZappiDayHistory.class);
        when(mockHistory.getReadings()).thenReturn(List.of(new ZappiHistory(2023, 1, 2, 9, 0,
                "Monday", 3600000L, 1800000L,
                360000L, 720000L, 7200000L)));
        var localDate = LocalDate.of(2023, 1, 20);
        when(mockClient.getZappiHistory(localDate.minusDays(1), 22))
                .thenReturn(mockHistory);
        zappiService.getEnergyUsage(localDate, ZoneId.of("Europe/Helsinki"));   // Eastern European Time

        verify(mockClient).getZappiHistory(localDate.minusDays(1), 22);
    }

    @Test
    void testGetEnergyUsageAdjustsToLocalTimeForTimeZoneWithNegativeOffset() {
        var mockHistory = mock(ZappiDayHistory.class);
        when(mockHistory.getReadings()).thenReturn(List.of(new ZappiHistory(2023, 1, 2, 9, 0,
                "Monday", 3600000L, 1800000L,
                360000L, 720000L, 7200000L)));
        var localDate = LocalDate.of(2023, 1, 20);
        when(mockClient.getZappiHistory(localDate, 5))
                .thenReturn(mockHistory);
        zappiService.getEnergyUsage(localDate, ZoneId.of("America/New_York"));

        verify(mockClient).getZappiHistory(localDate, 5);
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
                Arguments.of("11:32:30", Duration.of(1, ChronoUnit.HOURS), "1230", new KiloWattHour(12)),   // kWh is 12, 5 already in the EV and 7 to be added in an hour
                Arguments.of("23:32:30", Duration.of(1, ChronoUnit.HOURS), "0030", new KiloWattHour(12)),
                Arguments.of("23:40:30", Duration.of(1, ChronoUnit.HOURS), "0045", new KiloWattHour(12)),
                Arguments.of("23:44:30", Duration.of(1, ChronoUnit.HOURS), "0045", new KiloWattHour(12)),
                Arguments.of("09:30:30", Duration.of(1, ChronoUnit.HOURS), "1030", new KiloWattHour(12)),
                Arguments.of("01:00:59", Duration.of(1, ChronoUnit.HOURS), "0200", new KiloWattHour(12)),
                // round up
                Arguments.of("01:08:59", Duration.of(1, ChronoUnit.HOURS), "0215", new KiloWattHour(12)),
                Arguments.of("01:23:59", Duration.of(1, ChronoUnit.HOURS), "0230", new KiloWattHour(12)),
                Arguments.of("01:38:59", Duration.of(1, ChronoUnit.HOURS), "0245", new KiloWattHour(12)),
                Arguments.of("01:53:59", Duration.of(1, ChronoUnit.HOURS), "0300", new KiloWattHour(12)),
                // round down
                Arguments.of("01:07:59", Duration.of(1, ChronoUnit.HOURS), "0200", new KiloWattHour(12)),
                Arguments.of("01:22:59", Duration.of(1, ChronoUnit.HOURS), "0215", new KiloWattHour(12)),
                Arguments.of("01:37:59", Duration.of(1, ChronoUnit.HOURS), "0230", new KiloWattHour(12)),
                Arguments.of("01:52:59", Duration.of(1, ChronoUnit.HOURS), "0245", new KiloWattHour(12)),
                Arguments.of("01:52:59", Duration.of(7, ChronoUnit.HOURS), "0845", new KiloWattHour(56)));  // 7 hours * 7.3kW = 51.1kWh + 5 already in the EV == 56.1kWh, then floor is applied, making it 56.0
    }

    private static Stream<Arguments> boostWithEndTime() {
        return Stream.of(
                Arguments.of("00:32:30", "0030", new KiloWattHour(30)), // charge 3.5 hours (9PM to 12:30PM) * 7.3kW = 25.55kWh. There is already 5kWh in the EV, so 30kWh is sent to the charger
                Arguments.of("00:40:30", "0045", new KiloWattHour(32)),
                Arguments.of("00:44:30", "0045", new KiloWattHour(32)),
                Arguments.of("10:30:30", "1030", new KiloWattHour(99)),
                Arguments.of("02:00:59", "0200", new KiloWattHour(41)),
                // round up
                Arguments.of("02:08:59", "0215", new KiloWattHour(43)),
                Arguments.of("02:23:59", "0230", new KiloWattHour(45)),
                Arguments.of("02:38:59", "0245", new KiloWattHour(46)),
                Arguments.of("03:53:59", "0400", new KiloWattHour(56)),
                // round down
                Arguments.of("02:07:59", "0200", new KiloWattHour(41)),
                Arguments.of("02:22:59", "0215", new KiloWattHour(43)),
                Arguments.of("02:37:59", "0230", new KiloWattHour(45)),
                Arguments.of("02:52:59", "0245", new KiloWattHour(46)),
                Arguments.of("12:32:30", "1230", new KiloWattHour(99)));    // charge from 9PM to 12:30PM = 15.5 hours * 7.3kW = 113.15kWh. All values clamped between 0 and 99kWh so 99kWh is sent to the charger
    }
}
