package com.amcglynn.myzappi.login.rest;

import com.amcglynn.myzappi.core.model.Tariff;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TariffRequestValidator {

    private final List<String> SUPPORTED_CURRENCIES = List.of("EUR", "GBP");
    public void validate(String currency, List<Tariff> tariffs) {
        validateNotNullOrEmpty(tariffs);
        validateSize(tariffs);
        validateCurrency(currency);

        var hourlyTariffs = new HashMap<Integer, Tariff>();

        // loop over each and make sure there's no overlap
        for (var tariff : tariffs) {
            validateTariffName(tariff);
            validateTariffTimes(tariff);
            validateKwCost(tariff);

            // get all hours from start to end
            for (int i = tariff.getStartTime(); i < tariff.getEndTime(); i++) {
                var existingTariff = hourlyTariffs.get(i);
                if (existingTariff != null) {
                    log.info("Overlap found in tariff for time {}", i);
                    throw new ServerException(400);
                }
                hourlyTariffs.put(i, tariff);
            }
        }
        validateOnly24HoursAreCovered(hourlyTariffs);
    }

    private void validateTariffName(Tariff tariff) {
        if (tariff.getName().length() > 128) {
            log.info("Tariff name too long");
            throw new ServerException(400);
        }
    }

    private void validateCurrency(String currency) {
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            log.info("Unsupported currency {}", currency);
            throw new ServerException(400);
        }
    }

    private void validateKwCost(Tariff tariff) {
        if (tariff.getImportCostPerKwh() < 0.0 || tariff.getExportCostPerKwh() < 0.0) {
            log.info("Invalid import or export cost");
            throw new ServerException(400);
        }
    }

    private void validateTariffTimes(Tariff tariff) {
        validateStartTime(tariff);
        validateEndTime(tariff);
    }

    private void validateStartTime(Tariff tariff) {
        if (tariff.getStartTime() < 0 || tariff.getStartTime() > 23) {
            log.info("Invalid start time {}", tariff.getStartTime());
            throw new ServerException(400);
        }
    }

    private void validateEndTime(Tariff tariff) {
        if (tariff.getEndTime() < 1 || tariff.getEndTime() > 24) {
            log.info("Invalid end time {}", tariff.getEndTime());
            throw new ServerException(400);
        }
    }

    private void validateOnly24HoursAreCovered(Map<Integer, Tariff> hourlyTariffs) {
        if (hourlyTariffs.size() != 24) {
            log.info("Specified tariffs do not cover the complete day");
            throw new ServerException(400);
        }
    }

    private void validateNotNullOrEmpty(List<Tariff> tariffs) {
        if (tariffs == null || tariffs.isEmpty()) {
            log.info("Null or empty tariffs");
            throw new ServerException(400);
        }
    }

    private void validateSize(List<Tariff> tariffs) {
        if (tariffs.size() > 24) {
            log.info("Invalid tariff size {}", tariffs.size());
            throw new ServerException(400);
        }
    }
}
