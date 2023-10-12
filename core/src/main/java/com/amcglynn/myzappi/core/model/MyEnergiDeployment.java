package com.amcglynn.myzappi.core.model;

import java.nio.ByteBuffer;
import java.util.Optional;

public class MyEnergiDeployment {

    private final String userId;
    private final SerialNumber zappiSerialNumber;
    private final SerialNumber serialNumber;
    private final ByteBuffer encryptedApiKey;
    private final SerialNumber eddiSerialNumber;

    /**
     *
     * @param userId Amazon user ID
     * @param zappiSerialNumber serial number of the Zappi device
     * @param serialNumber serial number of the hub/gateway. This may or may not be the same as the zappi serial number
     * @param eddiSerialNumber serial number of the Eddi device. This may be null if the user does not have an Eddi
     * @param encryptedApiKey API key of the hub/gateway.
     */
    public MyEnergiDeployment(String userId, SerialNumber zappiSerialNumber, SerialNumber serialNumber, SerialNumber eddiSerialNumber, ByteBuffer encryptedApiKey) {
        this.userId = userId;
        this.zappiSerialNumber = zappiSerialNumber;
        this.serialNumber = serialNumber;
        this.encryptedApiKey = encryptedApiKey;
        this.eddiSerialNumber = eddiSerialNumber;
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

    /**
     * Get the Zappi serial number. This may or may not be the same as the hub/gateway, depending on the Zappi verion
     * and deployment.
     * @return serial number
     */
    public SerialNumber getZappiSerialNumber() {
        return zappiSerialNumber;
    }

    public Optional<SerialNumber> getEddiSerialNumber() {
        return Optional.ofNullable(eddiSerialNumber);
    }
}