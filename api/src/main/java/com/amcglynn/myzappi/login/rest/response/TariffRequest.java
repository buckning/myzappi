package com.amcglynn.myzappi.login.rest.response;

import com.amcglynn.myzappi.core.model.Tariff;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TariffRequest {
    private String currency;
    private List<Tariff> tariffs;
}
