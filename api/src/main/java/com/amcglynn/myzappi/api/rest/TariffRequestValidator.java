package com.amcglynn.myzappi.api.rest;

import com.amcglynn.myzappi.core.model.Tariff;
import com.amcglynn.myzappi.core.service.TariffService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class TariffRequestValidator {

    private final TariffService tariffService;

    public TariffRequestValidator(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    private final List<String> SUPPORTED_CURRENCIES = List.of("EUR", "GBP");
    public void validate(String currency, List<Tariff> tariffs) {
        validateNotNullOrEmpty(tariffs);
        validateSize(tariffs);
        validateCurrency(currency);

        // loop over each and make sure there's no overlap
        for (var tariff : tariffs) {
            validateTariffName(tariff);
            validateKwCost(tariff);
            validateTariff(tariff);
        }
        var listIntervals = tariffService.constructTariffList(tariffs);

        validateOnly24HoursAreCovered(tariffs, listIntervals);
    }

    private void validateTariff(Tariff tariff) {
        if (tariff.getStart() == null) {
            log.info("start is null");
            throw new ServerException(400);
        }

        if (tariff.getEnd() == null) {
            log.info("end is null");
            throw new ServerException(400);
        }
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

    private void validateOnly24HoursAreCovered(List<Tariff> tariffsFromRequest, List<Tariff> hourlyTariffs) {
        if (hourlyTariffs.contains(null)) {
            log.info("Specified tariffs do not cover the complete day {}", tariffsFromRequest);
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
        if (tariffs.size() > 48) {
            log.info("Invalid tariff size {}", tariffs.size());
            throw new ServerException(400);
        }
    }
}
