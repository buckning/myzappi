package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.response.EnergyCostResponse;
import com.amcglynn.myzappi.core.model.Cost;
import com.amcglynn.myzappi.core.service.Clock;
import com.amcglynn.myzappi.core.service.TariffService;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
public class EnergyCostController implements RestController {

    private final TariffService tariffService;
    private final ZappiService.Builder zappiServiceBuilder;
    private final Clock clock;

    public EnergyCostController(ZappiService.Builder zappiServiceBuilder, TariffService tariffService) {
        this.tariffService = tariffService;
        this.zappiServiceBuilder = zappiServiceBuilder;
        this.clock = new Clock();
    }

    @Override
    public Response handle(Request request) {
        if (RequestMethod.GET == request.getMethod()) {
            return getTariffs(request);
        }

        log.info("Unsupported method for energy cost - " + request.getMethod());
        throw new ServerException(404);
    }


    @SneakyThrows
    private Response getTariffs(Request request) {
        var dayTariff = tariffService.get(request.getUserId().toString());
        var zappiService = zappiServiceBuilder.build(() -> request.getUserId().toString());
        if (dayTariff.isEmpty()) {
            log.info("No tariffs configured for {}", request.getUserId());
            throw new ServerException(404);
        }

        var zoneId = getZoneId(request);
        var localDate = getLocalDate(request, zoneId);

        var history = zappiService.getHistory(localDate, zoneId);

        var cost = tariffService.calculateCost(dayTariff.get(), history, localDate, zoneId);

        var responseBody = new ObjectMapper().writeValueAsString(EnergyCostResponse.builder()
                        .currency(cost.getCurrency())
                .totalCost(to2DecimalPlaces(cost.getTotalCost()))
                .importCost(to2DecimalPlaces(cost.getImportCost()))
                .exportCost(to2DecimalPlaces(cost.getExportCost()))
                .solarConsumed(to2DecimalPlaces(cost.getSolarSavings()))
                .build());

        return new Response(200, responseBody);
    }

    private double to2DecimalPlaces(double value) {
        return new Cost("", value).to2DecimalPlaces();
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
