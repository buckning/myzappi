package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.units.KiloWatt;
import com.amcglynn.myenergi.units.Watt;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.core.model.DayCost;
import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.EnergyStatus;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.Tariff;
import com.amcglynn.myzappi.core.service.EnergyCostHourSummary;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.TariffService;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EnergyControllerTest {

    @Mock
    private MyEnergiService mockMyEnergiService;
    @Mock
    private MyEnergiService.Builder mockMyEnergiServiceBuilder;
    @Mock
    private TariffService mockTariffService;
    @Captor
    private ArgumentCaptor<Schedule> scheduleCaptor;
    private EnergyController controller;
    @Mock
    private ZappiService mockZappiService;

    @BeforeEach
    void setUp() {
        when(mockMyEnergiService.getZappiServiceOrThrow()).thenReturn(mockZappiService);
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        var dayCost = new DayCost("EUR");
        var tariff = new Tariff("MockTariff", LocalTime.of(0, 0), LocalTime.of(0, 0), 1, 1);
        dayCost.add(new EnergyCostHourSummary(tariff, new ZappiHistory(2023, 1, 6, 0, 0, "Monday",
                3600000L, 7200000L, 3600000L, 3600000L, 3600000L)));

        when(mockTariffService.get(anyString())).thenReturn(Optional.of(new DayTariff("EUR", List.of(tariff))));
        when(mockTariffService.calculateCost(any(), any(), any(), any())).thenReturn(dayCost);
        when(mockMyEnergiService.getEnergyStatus()).thenReturn(EnergyStatus.builder()
                        .consumingKW(new KiloWatt(new Watt(3500L)))
                .importingKW(new KiloWatt(new Watt(0L)))
                .exportingKW(new KiloWatt(new Watt(3700L)))
                .solarGenerationKW(new KiloWatt(new Watt(7200L)))
                .build());
        controller = new EnergyController(mockMyEnergiServiceBuilder, mockTariffService);
    }

    @Test
    void getReturns404WhenNoTariffsAreFound() {
        when(mockTariffService.get(any())).thenReturn(Optional.empty());

        Request request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of(), Map.of());
        request.setUserId("mockUserId");
        var serverException = catchThrowableOfType(() -> controller.getEnergyCost(request), ServerException.class);
        assertThat(serverException.getStatus()).isEqualTo(404);
    }

    @Test
    void get() {
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        Request request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of(), null);
        request.setUserId("mockUserId");
        var response = controller.getEnergyCost(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("{\"currency\":\"EUR\",\"importCost\":1.0," +
                "\"exportCost\":2.0,\"solarConsumed\":-1.0,\"totalCost\":-1.0}"));
    }

    @Test
    void getEnergySummary() {
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        Request request = new Request(RequestMethod.GET, "/energy-summary", null, Map.of(), null);
        request.setUserId("mockUserId");
        var response = controller.getEnergySummary(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("""
                {"solarGenerationKW":"7.2","consumingKW":"3.5","importingKW":"0.0","exportingKW":"3.7"}\
                """));
    }

    @Test
    void getWithDateQueryParam() {
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        Request request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of(), Map.of("date", "2023-01-06"));
        request.setUserId("mockUserId");
        var response = controller.getEnergyCost(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("{\"currency\":\"EUR\",\"importCost\":1.0," +
                "\"exportCost\":2.0,\"solarConsumed\":-1.0,\"totalCost\":-1.0}"));

        verify(mockZappiService).getHistory(LocalDate.of(2023, 1, 6), ZoneId.of("Europe/London"));
    }

    @Test
    void getWithZoneIdQueryParam() {
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        Request request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of(),
                Map.of("zoneId", "Europe%2FDublin",
                        "date", "2023-01-06"));
        request.setUserId("mockUserId");
        var response = controller.getEnergyCost(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("{\"currency\":\"EUR\",\"importCost\":1.0," +
                "\"exportCost\":2.0,\"solarConsumed\":-1.0,\"totalCost\":-1.0}"));

        verify(mockZappiService).getHistory(LocalDate.of(2023, 1, 6), ZoneId.of("Europe/Dublin"));
    }

    @Test
    void getEnergyUsageReturnsMinuteReadingsAsLocalisedEnergyAndPower() {
        when(mockZappiService.getHistory(LocalDate.of(2023, 1, 6), ZoneId.of("Europe/Dublin")))
                .thenReturn(List.of(new ZappiHistory(2023, 6, 1, 0, 1, "Friday",
                        3600000L, 600000L, 0L, 0L, 1200000L)));

        Request request = new Request(RequestMethod.GET, "/energy-usage", null, Map.of(), Map.of(
                "date", "2023-01-06",
                "zoneId", "Europe%2FDublin",
                "resolution", "minute"));
        request.setUserId("mockUserId");

        var response = controller.getEnergyUsage(request);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).hasValue("""
                {"date":"2023-01-06","zoneId":"Europe/Dublin","resolution":"minute","readings":[{"timestamp":"2023-01-06T00:01Z","solarGenerationKWh":1.0,"importedKWh":0.333333,"exportedKWh":0.166667,"consumedKWh":0.833333,"importedKW":20.0,"exportedKW":10.0,"consumedKW":50.0}]}\
                """);
    }

    @Test
    void getEnergyUsageMapsHourlyResolutionToHourlyHistory() {
        when(mockZappiService.getHourlyHistory(LocalDate.of(2023, 1, 6), ZoneId.of("Europe/London")))
                .thenReturn(List.of(new ZappiHistory(2023, 6, 1, 12, 0, "Friday",
                        7200000L, 3600000L, 0L, 0L, 1800000L)));

        Request request = new Request(RequestMethod.GET, "/energy-usage", null, Map.of(), Map.of(
                "date", "2023-01-06",
                "resolution", "hourly"));
        request.setUserId("mockUserId");

        var response = controller.getEnergyUsage(request);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).hasValue("""
                {"date":"2023-01-06","zoneId":"Europe/London","resolution":"hourly","readings":[{"timestamp":"2023-01-06T12:00Z","solarGenerationKWh":2.0,"importedKWh":0.5,"exportedKWh":1.0,"consumedKWh":1.0,"importedKW":0.5,"exportedKW":1.0,"consumedKW":1.0}]}\
                """);
        verify(mockZappiService).getHourlyHistory(LocalDate.of(2023, 1, 6), ZoneId.of("Europe/London"));
    }

    @Test
    void getEnergyUsageReturnsBadRequestForUnknownResolution() {
        Request request = new Request(RequestMethod.GET, "/energy-usage", null, Map.of(), Map.of("resolution", "daily"));
        request.setUserId("mockUserId");

        var serverException = catchThrowableOfType(() -> controller.getEnergyUsage(request), ServerException.class);

        assertThat(serverException.getStatus()).isEqualTo(400);
    }
}
