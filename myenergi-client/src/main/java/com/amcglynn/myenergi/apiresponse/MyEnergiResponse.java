package com.amcglynn.myenergi.apiresponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class MyEnergiResponse <T> {
    private boolean status;
    private String message;
    private String field;
    private T content;
}
