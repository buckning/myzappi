package com.amcglynn.myzappi.login.rest;

import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.service.TariffService;
import com.amcglynn.myzappi.login.rest.response.TariffRequest;
import com.amcglynn.myzappi.login.rest.response.TariffResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TariffController implements RestController {

    private TariffService tariffService;
    private TariffRequestValidatorV2 validator;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
        this.validator = new TariffRequestValidatorV2(tariffService);
    }


    @Override
    public Response handle(Request request) {
        if (RequestMethod.GET == request.getMethod()) {
            return getTariffs(request);
        }
        if (RequestMethod.POST == request.getMethod()) {
            return saveTariffs(request);
        }

        log.info("Unsupported method for tariff - " + request.getMethod());
        throw new ServerException(404);
    }

    private Response saveTariffs(Request request) {
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
    private Response getTariffs(Request request) {
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
