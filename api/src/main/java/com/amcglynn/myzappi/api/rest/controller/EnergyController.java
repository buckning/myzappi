package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.response.EnergyCostResponse;
import com.amcglynn.myzappi.api.rest.response.EnergyUsageResponse;
import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.core.service.Clock;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.TariffService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.stream.Collectors;

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

    @SneakyThrows
    public Response getEnergyUsage(Request request) {
        var zoneId = getZoneId(request);
        var localDate = getLocalDate(request, zoneId);
        var resolution = EnergyUsageResolution.from(request.getQueryStringParameters()
                .getOrDefault("resolution", EnergyUsageResolution.MINUTE.value));
        var zappiService = myEnergiServiceBuilder.build(() -> request.getUserId().toString()).getZappiServiceOrThrow();
        var history = resolution == EnergyUsageResolution.HOURLY
                ? zappiService.getHourlyHistory(localDate, zoneId)
                : zappiService.getHistory(localDate, zoneId);

        var readings = history.stream()
                .map(reading -> toEnergyUsageReading(reading, zoneId, resolution))
                .sorted((left, right) -> left.getTimestamp().compareTo(right.getTimestamp()))
                .collect(Collectors.toList());
        var responseBody = new ObjectMapper().writeValueAsString(new EnergyUsageResponse(
                localDate.toString(), zoneId.toString(), resolution.value, readings));

        return new Response(200, responseBody);
    }

    private EnergyUsageResponse.EnergyUsageReading toEnergyUsageReading(
            ZappiHistory reading, ZoneId zoneId, EnergyUsageResolution resolution) {
        var solarGenerationKWh = new KiloWattHour(reading.getSolarGeneration()).getDouble();
        var importedKWh = new KiloWattHour(reading.getImported()).getDouble();
        var exportedKWh = new KiloWattHour(reading.getGridExport()).getDouble();
        // The graph's consumed series represents solar energy used on site, matching
        // the Alexa visualisation. Grid imports remain a separate series.
        var consumedKWh = solarGenerationKWh - exportedKWh;
        var intervalHours = resolution == EnergyUsageResolution.MINUTE ? 1.0 / 60.0 : 1.0;
        var localTimestamp = toLocalTimestamp(reading, zoneId);

        return new EnergyUsageResponse.EnergyUsageReading(
                localTimestamp.toString(),
                round(solarGenerationKWh),
                round(importedKWh),
                round(exportedKWh),
                round(consumedKWh),
                round(importedKWh / intervalHours),
                round(exportedKWh / intervalHours),
                round(consumedKWh / intervalHours));
    }

    private OffsetDateTime toLocalTimestamp(ZappiHistory reading, ZoneId zoneId) {
        return LocalDateTime.of(reading.getYear(), reading.getMonth(), reading.getDayOfMonth(),
                        reading.getHour(), reading.getMinute())
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(zoneId)
                .toOffsetDateTime();
    }

    private double round(double value) {
        return Math.round(value * 1_000_000d) / 1_000_000d;
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

    private enum EnergyUsageResolution {
        MINUTE("minute"),
        HOURLY("hourly");

        private final String value;

        EnergyUsageResolution(String value) {
            this.value = value;
        }

        private static EnergyUsageResolution from(String value) {
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new ServerException(400);
            }
        }
    }
}
