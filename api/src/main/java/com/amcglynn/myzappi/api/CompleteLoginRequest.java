package com.amcglynn.myzappi.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CompleteLoginRequest {
    private String serialNumber;
    private String apiKey;
}
