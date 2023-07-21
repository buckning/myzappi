package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myzappi.core.dal.TariffRepository;
import com.amcglynn.myzappi.core.model.DayCost;
import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.Tariff;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TariffService {

    private final TariffRepository tariffRepository;

    public TariffService(TariffRepository tariffRepository) {
        this.tariffRepository = tariffRepository;
    }

    public Optional<DayTariff> get(String userId) {
        return tariffRepository.read(userId);
    }

    private int convertHourToUtc(int hour, LocalDate localDate, ZoneId zoneId) {
        var localTime = LocalTime.of(hour, 0);
        var zonedDateTime = ZonedDateTime.of(localDate, localTime, zoneId);
        return zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalTime().getHour();
    }

    public void write(String userId, DayTariff dayTariff) {
        tariffRepository.write(userId, dayTariff);
    }

    /**
     * ZappiHistory is in UTC. Tariffs are stored as localtime. So this means that timezones and DST impact the cost of
     * the calculation. To get accurate costs, the local time of the user is needed and also the date.
     * @param tariffFromDb user tariffs from the DB in Local time
     * @param hourlyEnergyUsage Zappi History in UTC
     * @param localDate date of the cost calculation
     * @param zoneId time zone ID of the user
     * @return calculated cost
     */
    public DayCost calculateCost(DayTariff tariffFromDb, List<ZappiHistory> hourlyEnergyUsage,
                                 LocalDate localDate, ZoneId zoneId) {
        var tariffMap = buildTariffMap(tariffFromDb.getTariffs(), localDate, zoneId);
        var dayCost = new DayCost(tariffFromDb.getCurrency());

        for (var history : hourlyEnergyUsage) {
            var tariff = tariffMap.get(history.getHour());
            var energyCost = new EnergyCostHourSummary(tariff, history);
            dayCost.add(energyCost);
        }
        return dayCost;
    }

    private Map<Integer, Tariff> buildTariffMap(List<Tariff> tariffs, LocalDate localDate, ZoneId zoneId) {
        var hourlyTariffs = new HashMap<Integer, Tariff>();
        for (var tariff : tariffs) {
            for (int i = tariff.getStartTime(); i < tariff.getEndTime(); i++) {
                // convert tariff to UTC so it easier to calculate cost against ZappiHistory since ZappiHistory is in UTC
                var utcHour = convertHourToUtc(i, localDate, zoneId);
                hourlyTariffs.put(utcHour, tariff);
            }
        }
        return hourlyTariffs;
    }
}
