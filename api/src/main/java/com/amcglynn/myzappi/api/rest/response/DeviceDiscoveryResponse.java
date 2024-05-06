package com.amcglynn.myzappi.api.rest.response;

import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.SerialNumberDeserializer;
import com.amcglynn.myzappi.core.model.SerialNumberSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DeviceDiscoveryResponse {
    @JsonDeserialize(using = SerialNumberDeserializer.class)
    @JsonSerialize(using = SerialNumberSerializer.class)
    private SerialNumber serialNumber;
}
