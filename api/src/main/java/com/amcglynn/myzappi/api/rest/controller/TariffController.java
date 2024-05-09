package com.amcglynn.myzappi.api.rest.controller;

import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.service.TariffService;
import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.RequestMethod;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.api.rest.validator.TariffRequestValidator;
import com.amcglynn.myzappi.api.rest.response.TariffRequest;
import com.amcglynn.myzappi.api.rest.response.TariffResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TariffController {

    private TariffService tariffService;
    private TariffRequestValidator validator;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
        this.validator = new TariffRequestValidator(tariffService);
    }

    public Response saveTariffs(Request request) {
        try {
            var tariffRequest = new ObjectMapper().readValue(request.getBody(), new TypeReference<TariffRequest>() {
            });
            validator.validate(tariffRequest.getCurrency(), tariffRequest.getTariffs());
            tariffService.write(request.getUserId().toString(), new DayTariff(tariffRequest.getCurrency(), tariffRequest.getTariffs()));
            return new Response(200);
        } catch (JsonProcessingException e) {
            log.info("Invalid tariff POST request");
            throw new ServerException(400);
        }
    }

    @SneakyThrows
    public Response getTariffs(Request request) {
        var tariffs = tariffService.get(request.getUserId().toString());

        if (tariffs.isEmpty()) {
            log.info("No tariff configured for {}", request.getUserId());
            throw new ServerException(404);
        }
        var body = new ObjectMapper().writeValueAsString(new TariffResponse(tariffs.get().getCurrency(),
                tariffs.get().getTariffs()));
        return new Response(200, body);
    }
}
