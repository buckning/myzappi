package com.amcglynn.myzappi.login;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CompleteLoginRequest {
    private String loginCode;
    private String serialNumber;
    private String apiKey;
}
