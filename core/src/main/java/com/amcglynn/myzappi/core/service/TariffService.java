package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myzappi.core.dal.TariffRepository;
import com.amcglynn.myzappi.core.model.DayCost;
import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.Tariff;

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

    public void write(String userId, DayTariff dayTariff) {
        tariffRepository.write(userId, dayTariff);
    }

    public DayCost calculateCost(DayTariff tariffFromDb, List<ZappiHistory> hourlyEnergyUsage) {
        var tariffMap = buildTariffMap(tariffFromDb.getTariffs());
        var dayCost = new DayCost(tariffFromDb.getCurrency());

        for (var history : hourlyEnergyUsage) {
            var tariff = tariffMap.get(history.getHour());
            var energyCost = new EnergyCostHourSummary(tariff, history);
            dayCost.add(energyCost);
        }
        return dayCost;
    }

    private Map<Integer, Tariff> buildTariffMap(List<Tariff> tariffs) {
        var hourlyTariffs = new HashMap<Integer, Tariff>();
        for (var tariff : tariffs) {
            for (int i = tariff.getStartTime(); i < tariff.getEndTime(); i++) {
                hourlyTariffs.put(i, tariff);
            }
        }
        return hourlyTariffs;
    }
}
