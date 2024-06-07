package com.amcglynn.myzappi.core.model;

import com.amcglynn.myenergi.LibbiState;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LibbiStatus {
    @JsonDeserialize(using = SerialNumberDeserializer.class)
    @JsonSerialize(using = SerialNumberSerializer.class)
    private SerialNumber serialNumber;
    private LibbiState state;
    private int stateOfChargePercentage;
    private KiloWattHour batterySizeKWh;
    // below fields are optional
    private Boolean chargeFromGridEnabled;
    private KiloWattHour energyTargetKWh;
}
