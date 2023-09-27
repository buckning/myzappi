package com.amcglynn.myzappi.api.rest.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class EnergyCostResponse {
    private String currency;
    private double importCost;
    private double exportCost;
    private double solarConsumed;
    private double totalCost;
}
