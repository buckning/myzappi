package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myzappi.core.dal.TariffRepository;
import com.amcglynn.myzappi.core.model.DayCost;
import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.Tariff;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TariffService {

    private final TariffRepository tariffRepository;

    public TariffService(TariffRepository tariffRepository) {
        this.tariffRepository = tariffRepository;
    }

    public Optional<DayTariff> get(String userId) {
        return tariffRepository.read(userId);
    }

    private LocalTime convertToLocalTime(LocalTime localTime, LocalDate localDate, ZoneId zoneId) {
        var zonedDateTime = ZonedDateTime.of(localDate, localTime, ZoneId.of("UTC"));
        return zonedDateTime.withZoneSameInstant(zoneId).toLocalTime();
    }

    public void write(String userId, DayTariff dayTariff) {
        tariffRepository.write(userId, dayTariff);
    }

    public DayCost calculateCost(DayTariff tariffFromDb, List<ZappiHistory> hourlyEnergyUsage,
                                 LocalDate localDate, ZoneId zoneId) {
        var dayCost = new DayCost(tariffFromDb.getCurrency());
        var tariffList = constructTariffList(tariffFromDb.getTariffs());

        for (var history : hourlyEnergyUsage) {
            // zappi history is UTC but tariffs are in local time, they need to be converted before using
            var localHistoryTime = convertToLocalTime(LocalTime.of(history.getHour(), history.getMinute()), localDate, zoneId);

            var tariff = getTariff(localHistoryTime, tariffList);
            var energyCost = new EnergyCostHourSummary(tariff, history);
            dayCost.add(energyCost);
        }
        return dayCost;
    }

    public Tariff getTariff(LocalTime localTime, List<Tariff> tariffList) {
        // This variable controls what type of resolution is used for tariffs. 1 means there is just 1 tariff per hour, 2 means 2 per hour or one per 30 mins
        int resolution = 2; // should be less than or equal to 6 (10 minutes). It must be a number that evenly divides into 60.

        int blockSize = 60 / resolution;

        return tariffList.get((int)(Duration.between(LocalTime.of(0, 0), localTime).toMinutes() / blockSize));
    }

    public List<Tariff> constructTariffList(List<Tariff> tariffsFromDb) {
        List<Tariff> results = new ArrayList<>();

        // This variable controls what type of resolution is used for tariffs. 1 means there is just 1 tariff per hour, 2 means 2 per hour or one per 30 mins
        int resolution = 2; // should be less than or equal to 6 (10 minutes). It must be a number that evenly divides into 60.

        int blockSize = 60 / resolution;

        for (var tariff : tariffsFromDb) {
            var duration = Duration.between(tariff.getStart(), tariff.getEnd());

            var minutes = duration.toMinutes();

            if (minutes < 0) {
                // local time does not have date associated with it so when the end time is 0:00, the duration doesn't
                // roll into the next day, it treats it as a negative duration, so we need to add 24 hours in minutes to
                // the calculation to fix it.
                minutes += Duration.ofDays(1).toMinutes();
            }

            var stepCount = minutes / blockSize;
            for (int i = 0; i < stepCount; i++) {
                results.add(tariff);
            }
        }

        return results;
    }
}
