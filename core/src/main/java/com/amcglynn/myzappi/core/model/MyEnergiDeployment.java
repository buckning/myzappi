package com.amcglynn.myzappi.core.model;

import java.nio.ByteBuffer;

public class MyEnergiDeployment {

    private final String userId;
    private final SerialNumber serialNumber;
    private final ByteBuffer encryptedApiKey;

    /**
     *
     * @param userId Amazon user ID
     * @param serialNumber serial number of the hub/gateway. This may or may not be the same as the zappi serial number
     * @param encryptedApiKey API key of the hub/gateway.
     */
    public MyEnergiDeployment(String userId, SerialNumber serialNumber, ByteBuffer encryptedApiKey) {
        this.userId = userId;
        this.serialNumber = serialNumber;
        this.encryptedApiKey = encryptedApiKey;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * Get the hub/gateway serial number.
     * @return serial number
     */
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
