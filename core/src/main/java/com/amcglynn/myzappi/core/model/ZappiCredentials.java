package com.amcglynn.myzappi.core.model;

import java.nio.ByteBuffer;

public class ZappiCredentials {

    private final String userId;
    private final SerialNumber serialNumber;
    private final ByteBuffer encryptedApiKey;

    /**
     * Used for a user where they have successfully completed the login process.
     * @param userId AWS userId
     * @param serialNumber Zappi serial number
     * @param encryptedApiKey Encrypted Zappi API key
     */
    public ZappiCredentials(String userId, SerialNumber serialNumber, ByteBuffer encryptedApiKey) {
        this.userId = userId;
        this.serialNumber = serialNumber;
        this.encryptedApiKey = encryptedApiKey;
    }

    public String getUserId() {
        return userId;
    }

    public SerialNumber getSerialNumber() {
        return serialNumber;
    }

    /**
     * Get the encrypted API key.
     * @return The encrypted API key. Note that the API key will be null if the user has not completed the
     * login process
     */
    public ByteBuffer getEncryptedApiKey() {
        return encryptedApiKey;
    }
}
