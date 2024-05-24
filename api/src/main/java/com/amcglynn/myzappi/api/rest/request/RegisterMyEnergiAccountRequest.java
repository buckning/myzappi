package com.amcglynn.myzappi.api.rest.request;

import lombok.Data;

@Data
public class RegisterMyEnergiAccountRequest {
    private String email;
    private String password;
}
