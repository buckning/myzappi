package com.amcglynn.myenergi.apiresponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class LibbiChargeSetupResponse {
//    {"deviceSerial":"30000001","chargeFromGrid":false,"energyTarget":5520}
    private String deviceSerial;
    private boolean chargeFromGrid;
    private int energyTarget;
}
