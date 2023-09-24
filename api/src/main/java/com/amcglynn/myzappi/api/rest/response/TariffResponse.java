package com.amcglynn.myzappi.api.rest.response;

import com.amcglynn.myzappi.core.model.Tariff;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TariffResponse {
    private String currency;
    private List<Tariff> tariffs;
}
