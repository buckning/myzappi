package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myzappi.core.dal.TariffRepository;
import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.Tariff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

    @Mock
    private TariffRepository tariffRepository;
    private TariffService service;
    private final LocalDate localDate = LocalDate.of(2023, 1, 1);
    private final LocalDate localDateWithDst = LocalDate.of(2023, 6, 1);    // DST changed on March 26th 2023.
    private final ZoneId zoneId = ZoneId.of("Europe/Dublin");

    @BeforeEach
    void setUp() {
        service = new TariffService(tariffRepository);
    }

    @Test
    void testReadReturnsEmptyOptionalWhenNothingInDbForUser() {
        assertThat(service.get("unknownUser")).isNotNull().isEmpty();
    }

    @Test
    void testReadReturnsOptionalOfValueFromDb() {
        var dayTariff = new DayTariff("EUR", List.of());
        when(tariffRepository.read("user")).thenReturn(Optional.of(dayTariff));
        assertThat(service.get("user")).isNotNull().contains(dayTariff);
    }

    @Test
    void testWrite() {
        var dayTariff = new DayTariff("EUR", List.of());
        service.write("user", dayTariff);
        verify(tariffRepository).write("user", dayTariff);
    }

    @Test
    void calculateCost() {
        var dayTariff = new DayTariff("EUR",
                List.of(new Tariff("Tariff1", LocalTime.of(0, 0), LocalTime.of(8, 0), 0.5, 0.25),
                        new Tariff("Tariff2", LocalTime.of(8, 0), LocalTime.of(0, 0), 0.75, 0.5)));

        List<ZappiHistory> hourlyHistory = List.of(getZappiHistory(0),
                getZappiHistory(1),
                getZappiHistory(2));
        var dayCost = service.calculateCost(dayTariff, hourlyHistory, localDate, zoneId);

        // import is 7.5kWh (3 x 9000000L = (2.5kWh). Import tariff = 0.5. Cost = 7.5 x 0.5 = 3.75
        assertThat(dayCost.getImportCost()).isEqualTo(3.75);
        // export is 3kWh (3 x 3600000L = (3kWh). Import tariff = 0.25. Cost = 3 x 0.25 = 0.75
        assertThat(dayCost.getExportCost()).isEqualTo(0.75);

        // solar generation = 9 kWh (3 x 10800000L = (9 kWh))
        // export is 3 kWh so solar consumed = 6 kWh. 6 kWh * 0.5 tariff = EUR 3.0
        assertThat(dayCost.getSolarSavings()).isEqualTo(3.0);
        assertThat(dayCost.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void calculateCostForSecondTariff() {
        var dayTariff = new DayTariff("EUR",
                List.of(new Tariff("Tariff1", LocalTime.of(0, 0), LocalTime.of(8, 0), 0.5, 0.25),
                        new Tariff("Tariff2", LocalTime.of(8, 0), LocalTime.of(0, 0), 0.75, 0.5)));

        List<ZappiHistory> hourlyHistory = List.of(getZappiHistory(10),
                getZappiHistory(11),
                getZappiHistory(12));
        var dayCost = service.calculateCost(dayTariff, hourlyHistory, localDate, zoneId);

        // import is 7.5kWh (3 x 9000000L = (2.5kWh). Import tariff = 0.75. Cost = 7.5 x 0.75 = 5.625
        assertThat(dayCost.getImportCost()).isEqualTo(5.625);
        // export is 3kWh (3 x 3600000L = (3kWh). Export tariff = 0.5. Cost = 3 x 0.5 = 1.5
        assertThat(dayCost.getExportCost()).isEqualTo(1.5);

        // solar generation = 9 kWh (3 x 10800000L = (9 kWh))
        // export is 3 kWh so solar consumed = 6 kWh. 6 kWh * 0.75 tariff = EUR 4.5
        assertThat(dayCost.getSolarSavings()).isEqualTo(4.5);
        assertThat(dayCost.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void calculateCostForBothTariffs() {
        var dayTariff = new DayTariff("EUR",
                List.of(new Tariff("Tariff1", LocalTime.of(0, 0), LocalTime.of(8, 0), 0.5, 0.25),
                        new Tariff("Tariff2", LocalTime.of(8, 0), LocalTime.of(0, 0), 0.75, 0.5)));

        List<ZappiHistory> hourlyHistory = List.of(getZappiHistory(0),
                getZappiHistory(1),
                getZappiHistory(2),
                getZappiHistory(10),
                getZappiHistory(11),
                getZappiHistory(12));
        var dayCost = service.calculateCost(dayTariff, hourlyHistory, localDate, zoneId);

        assertThat(dayCost.getImportCost()).isEqualTo(9.375);   // this is a combination of import costs from the previous 2 tests
        assertThat(dayCost.getExportCost()).isEqualTo(2.25);    // this is a combination of export costs from the previous 2 tests
        assertThat(dayCost.getSolarSavings()).isEqualTo(7.5);   // this is a combination of solar savings from the previous 2 tests
        assertThat(dayCost.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void calculateCostForDst() {
        var dayTariff = new DayTariff("EUR",
                List.of(new Tariff("Tariff1", LocalTime.of(0, 0), LocalTime.of(8, 0), 0.0, 0.0),
                        new Tariff("Tariff2", LocalTime.of(8, 0), LocalTime.of(0, 0), 0.75, 0.5)));

        List<ZappiHistory> hourlyHistory = List.of(getZappiHistory(7));    // 7 UTC is 8 local time so Tariff2 should be used
        var dayCost = service.calculateCost(dayTariff, hourlyHistory, localDateWithDst, zoneId);

        assertThat(dayCost.getExportCost()).isEqualTo(0.5);

        assertThat(dayCost.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void calculateCostWithoutDst() {
        // this is the same as the previous test but the date is different. DST changed on March 26th 2023.
        var dayTariff = new DayTariff("EUR",
                List.of(new Tariff("Tariff1", LocalTime.of(0, 0), LocalTime.of(8, 0), 0.0, 0.0),
                        new Tariff("Tariff2", LocalTime.of(8, 0), LocalTime.of(0, 0), 0.75, 0.5)));

        List<ZappiHistory> hourlyHistory = List.of(getZappiHistory(7, 5));    // 7 UTC is 8 local time so Tariff2 should be used
        var dayCost = service.calculateCost(dayTariff, hourlyHistory, localDate, zoneId);

        assertThat(dayCost.getExportCost()).isEqualTo(0.0);

        assertThat(dayCost.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void calculateCostWithHistorySpanningTwoTariffs() {
        var dayTariff = new DayTariff("EUR",
                List.of(new Tariff("Night1", LocalTime.of(0, 0), LocalTime.of(8, 0), 0.0, 0.0),
                        new Tariff("Day1", LocalTime.of(8, 0), LocalTime.of(17, 0), 1, 5),
                        new Tariff("Peak", LocalTime.of(17, 0), LocalTime.of(19, 0), 7, 20),
                        new Tariff("Day2", LocalTime.of(19, 0), LocalTime.of(23, 0), 3, 3),
                        new Tariff("Night2", LocalTime.of(23, 0), LocalTime.of(0, 0), 0.5, 0.5)));

        var hourlyHistory = new ArrayList<ZappiHistory>();
        IntStream.range(0, 8).forEach(i -> hourlyHistory.add(getZappiHistoryWith1KWH(i, 59)));
        var dayCost = service.calculateCost(dayTariff, hourlyHistory, localDateWithDst, zoneId);
        // zappi history hours are from 0 - 8 UTC (1 - 9 local), 7 hours in Night1 and 1 hour in Day1
        assertThat(dayCost.getImportCost()).isEqualTo(1);
        assertThat(dayCost.getExportCost()).isEqualTo(5);
    }

    @Test
    void testBuildList() {
        var tariff1 = new Tariff("Night1", LocalTime.of(0, 0), LocalTime.of(8, 0), 0.0, 0.0);
        Tariff tariff2 = new Tariff("Day1", LocalTime.of(8, 0), LocalTime.of(17, 0), 1, 5);
        Tariff tariff3 = new Tariff("Peak", LocalTime.of(17, 0), LocalTime.of(19, 0), 7, 20);
        Tariff tariff4 = new Tariff("Day2", LocalTime.of(19, 0), LocalTime.of(23, 0), 3, 3);
        Tariff tariff5 = new Tariff("Night2", LocalTime.of(23, 0), LocalTime.of(0, 0), 0.5, 0.5);
        var tariffsFromDb = List.of(tariff1,
                tariff2,
                tariff3,
                tariff4,
                tariff5);
        var results = service.constructTariffList(tariffsFromDb);
        assertThat(results).hasSize(48);

        for (int i = 0; i < 16; i++) {
            assertThat(results.get(i)).isEqualTo(tariff1);
        }

        for (int i = 16; i < 34; i++) {
            assertThat(results.get(i)).isEqualTo(tariff2);
        }

        for (int i = 34; i < 38; i++) {
            assertThat(results.get(i)).isEqualTo(tariff3);
        }

        for (int i = 38; i < 46; i++) {
            assertThat(results.get(i)).isEqualTo(tariff4);
        }

        for (int i = 46; i < 48; i++) {
            assertThat(results.get(i)).isEqualTo(tariff5);
        }
    }

    @Test
    void testGetIndex() {
        var tariff1 = new Tariff("Night1", LocalTime.of(0, 0), LocalTime.of(8, 30), 0.0, 0.0);
        Tariff tariff2 = new Tariff("Day1", LocalTime.of(8, 30), LocalTime.of(17, 0), 1, 5);
        Tariff tariff3 = new Tariff("Peak", LocalTime.of(17, 0), LocalTime.of(19, 0), 7, 20);
        Tariff tariff4 = new Tariff("Day2", LocalTime.of(19, 0), LocalTime.of(23, 0), 3, 3);
        Tariff tariff5 = new Tariff("Night2", LocalTime.of(23, 0), LocalTime.of(0, 0), 0.5, 0.5);
        var tariffs = List.of(tariff1,
                tariff2,
                tariff3,
                tariff4,
                tariff5);

        var tariffList = service.constructTariffList(tariffs);

        assertThat(service.getTariff(LocalTime.of(0, 0), tariffList)).isEqualTo(tariff1);
        assertThat(service.getTariff(LocalTime.of(4, 30), tariffList)).isEqualTo(tariff1);
        assertThat(service.getTariff(LocalTime.of(8, 29), tariffList)).isEqualTo(tariff1);
        assertThat(service.getTariff(LocalTime.of(8, 30), tariffList)).isEqualTo(tariff2);
        assertThat(service.getTariff(LocalTime.of(12, 0), tariffList)).isEqualTo(tariff2);
        assertThat(service.getTariff(LocalTime.of(16, 59), tariffList)).isEqualTo(tariff2);
        assertThat(service.getTariff(LocalTime.of(17, 0), tariffList)).isEqualTo(tariff3);
        assertThat(service.getTariff(LocalTime.of(18, 0), tariffList)).isEqualTo(tariff3);
        assertThat(service.getTariff(LocalTime.of(18, 59), tariffList)).isEqualTo(tariff3);
        assertThat(service.getTariff(LocalTime.of(19, 0), tariffList)).isEqualTo(tariff4);
        assertThat(service.getTariff(LocalTime.of(21, 0), tariffList)).isEqualTo(tariff4);
        assertThat(service.getTariff(LocalTime.of(22, 59), tariffList)).isEqualTo(tariff4);
        assertThat(service.getTariff(LocalTime.of(23, 0), tariffList)).isEqualTo(tariff5);
        assertThat(service.getTariff(LocalTime.of(23, 30), tariffList)).isEqualTo(tariff5);
        assertThat(service.getTariff(LocalTime.of(23, 59), tariffList)).isEqualTo(tariff5);
    }

    private ZappiHistory getZappiHistory(int hour) {
        return new ZappiHistory(2023, 1, 1, hour, 0, "Monday", 10800000L, 3600000L,
                1800000L, 7200000L, 9000000L);

    }

    private ZappiHistory getZappiHistory(int hour, int minute) {
        return new ZappiHistory(2023, 1, 1, hour, minute, "Monday", 10800000L, 3600000L,
                1800000L, 7200000L, 9000000L);

    }

    private ZappiHistory getZappiHistoryWith1KWH(int hour, int minute) {
        return new ZappiHistory(2023, 1, 1, hour, minute, "Monday", 3600000L, 3600000L,
                3600000L, 3600000L, 3600000L);

    }
}
