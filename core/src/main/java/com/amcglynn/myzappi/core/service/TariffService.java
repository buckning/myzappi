package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.TariffRepository;
import com.amcglynn.myzappi.core.model.DayTariff;

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
}
