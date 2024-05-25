package com.amcglynn.myzappi.api.rest.request;

import lombok.Data;

@Data
public class SetLibbiChargeFromGridRequest {
    private String email;
    private String password;
    private Boolean chargeFromGrid;
}
