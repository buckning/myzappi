package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.core.model.DayCost;
import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.ScheduleRecurrence;
import com.amcglynn.myzappi.core.model.Tariff;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.EnergyCostHourSummary;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EnergyCostControllerTest {

    @Mock
    private ZappiService.Builder mockZappiServiceBuilder;
    @Mock
    private TariffService mockTariffService;
    @Captor
    private ArgumentCaptor<Schedule> scheduleCaptor;
    private EnergyCostController controller;
    @Mock
    private ZappiService mockZappiService;

    @BeforeEach
    void setUp() {
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockZappiService);
        var dayCost = new DayCost("EUR");
        var tariff = new Tariff("MockTariff", LocalTime.of(0, 0), LocalTime.of(0, 0), 1, 1);
        dayCost.add(new EnergyCostHourSummary(tariff, new ZappiHistory(2023, 1, 6, 0, 0, "Monday",
                3600000L, 7200000L, 3600000L, 3600000L, 3600000L)));

        when(mockTariffService.get(anyString())).thenReturn(Optional.of(new DayTariff("EUR", List.of(tariff))));
        when(mockTariffService.calculateCost(any(), any(), any(), any())).thenReturn(dayCost);
        controller = new EnergyCostController(mockZappiServiceBuilder, mockTariffService);
    }

    @Test
    void getReturns404WhenNoTariffsAreFound() {
        when(mockTariffService.get(any())).thenReturn(Optional.empty());

        Request request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of(), Map.of());
        request.setUserId("mockUserId");
        var serverException = catchThrowableOfType(() -> controller.handle(request), ServerException.class);
        assertThat(serverException.getStatus()).isEqualTo(404);
    }

    @Test
    void get() {
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockZappiService);
        Request request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of(), null);
        request.setUserId("mockUserId");
        var response = controller.handle(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("{\"currency\":\"EUR\",\"importCost\":1.0," +
                "\"exportCost\":2.0,\"solarConsumed\":-1.0,\"totalCost\":-1.0}"));
    }

    @Test
    void getWithDateQueryParam() {
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockZappiService);
        Request request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of(), Map.of("date", "2023-01-06"));
        request.setUserId("mockUserId");
        var response = controller.handle(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("{\"currency\":\"EUR\",\"importCost\":1.0," +
                "\"exportCost\":2.0,\"solarConsumed\":-1.0,\"totalCost\":-1.0}"));

        verify(mockZappiService).getHistory(LocalDate.of(2023, 1, 6), ZoneId.of("Europe/London"));
    }

    @Test
    void getWithZoneIdQueryParam() {
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockZappiService);
        Request request = new Request(RequestMethod.GET, "/energy-cost", null, Map.of(),
                Map.of("zoneId", "Europe%2FDublin",
                        "date", "2023-01-06"));
        request.setUserId("mockUserId");
        var response = controller.handle(request);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Optional.of("{\"currency\":\"EUR\",\"importCost\":1.0," +
                "\"exportCost\":2.0,\"solarConsumed\":-1.0,\"totalCost\":-1.0}"));

        verify(mockZappiService).getHistory(LocalDate.of(2023, 1, 6), ZoneId.of("Europe/Dublin"));
    }
}
