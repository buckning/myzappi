package com.amcglynn.myzappi.core.model;

import lombok.Getter;

@Getter
public class MyEnergiAccountCredentials {

    private final String userId;
    private final String emailAddress;
    private final String password;

    public MyEnergiAccountCredentials(String userId, String emailAddress, String password) {
        this.userId = userId;
        this.emailAddress = emailAddress;
        this.password = password;
    }
}
