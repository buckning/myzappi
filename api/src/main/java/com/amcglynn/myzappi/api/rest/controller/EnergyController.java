package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.response.EnergyCostResponse;
import com.amcglynn.myzappi.core.service.Clock;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.TariffService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
public class EnergyController {

    private final TariffService tariffService;
    private final MyEnergiService.Builder myEnergiServiceBuilder;
    private final Clock clock;

    public EnergyController(MyEnergiService.Builder myEnergiServiceBuilder, TariffService tariffService) {
        this.tariffService = tariffService;
        this.myEnergiServiceBuilder = myEnergiServiceBuilder;
        this.clock = new Clock();
    }

    @SneakyThrows
    public Response getEnergyCost(Request request) {
        var dayTariff = tariffService.get(request.getUserId().toString());
        var zappiService = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
        if (dayTariff.isEmpty()) {
            log.info("No tariffs configured for {}", request.getUserId());
            throw new ServerException(404);
        }

        var zoneId = getZoneId(request);
        var localDate = getLocalDate(request, zoneId);

        var history = zappiService.getZappiServiceOrThrow().getHistory(localDate, zoneId);

        var cost = tariffService.calculateCost(dayTariff.get(), history, localDate, zoneId);

        var responseBody = new ObjectMapper().writeValueAsString(EnergyCostResponse.builder()
                .currency(cost.getCurrency())
                .totalCost(cost.getTotalCost())
                .importCost(cost.getImportCost())
                .exportCost(cost.getExportCost())
                .solarConsumed(cost.getSolarSavings())
                .build());

        return new Response(200, responseBody);
    }

    @SneakyThrows
    public Response getEnergySummary(Request request) {
        var myEnergiService = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
        var energySummary = myEnergiService.getEnergyStatus();
        var responseBody = new ObjectMapper().writeValueAsString(energySummary);

        return new Response(200, responseBody);
    }

    private LocalDate getLocalDate(Request request, ZoneId zoneId) {
        return LocalDate.parse(request.getQueryStringParameters()
                .getOrDefault("date", clock.localDate(zoneId).toString()));
    }

    private ZoneId getZoneId(Request request) {
        var zoneIdStr = URLDecoder.decode(request.getQueryStringParameters()
                .getOrDefault("zoneId", "Europe%2FLondon"), StandardCharsets.UTF_8);
        return ZoneId.of(zoneIdStr);
    }
}
