package com.amcglynn.myzappi.core.model;

import lombok.Getter;

import java.nio.ByteBuffer;

@Getter
public class MyEnergiAccountCredentials {

    private final String userId;
    private final ByteBuffer encryptedEmailAddress;
    private final ByteBuffer encryptedPassword;

    /**
     *
     * @param userId Amazon user ID
     * @param encryptedEmailAddress Encrypted email address
     *                              This is the email address used to login to the myenergi account
     * @param encryptedPassword Encrypted password
     */
    public MyEnergiAccountCredentials(String userId, ByteBuffer encryptedEmailAddress, ByteBuffer encryptedPassword) {
        this.userId = userId;
        this.encryptedEmailAddress = encryptedEmailAddress;
        this.encryptedPassword = encryptedPassword;
    }
}
