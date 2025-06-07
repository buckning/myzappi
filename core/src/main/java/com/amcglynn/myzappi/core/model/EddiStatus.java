package com.amcglynn.myzappi.core.model;

import com.amcglynn.myenergi.EddiState;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myenergi.units.Watt;
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
public class EddiStatus {
    @JsonDeserialize(using = SerialNumberDeserializer.class)
    @JsonSerialize(using = SerialNumberSerializer.class)
    private SerialNumber serialNumber;
    private EddiState state;
    private String activeHeater;
    private Watt gridImport;
    private Watt gridExport;
    private Watt consumed;
    private Watt generated;

    private KiloWattHour consumedThisSessionKWh;
}
