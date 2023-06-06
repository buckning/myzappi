package com.amcglynn.myzappi.core;

import java.nio.ByteBuffer;
import java.util.Optional;

public class ZappiCredentials {

    private final String userId;
    private final String serialNumber;
    private final String code;
    private final ByteBuffer encryptedApiKey;

    /**
     * Used when no API key is configured. In this case, the login process has started but the user has not completed
     * it by entering their code to the My Zappi website.
     * @param userId AWS userId
     * @param serialNumber Zappi serial number
     * @param code generated My Zappi login code
     */
    public ZappiCredentials(String userId, String serialNumber, String code) {
        this.userId = userId;
        this.serialNumber = serialNumber;
        this.code = code;
        this.encryptedApiKey = null;
    }

    /**
     * Used for a user where they have successfully completed the login process.
     * @param userId AWS userId
     * @param serialNumber Zappi serial number
     * @param code generated My Zappi login code
     * @param encryptedApiKey Encrypted Zappi API key
     */
    public ZappiCredentials(String userId, String serialNumber, String code, ByteBuffer encryptedApiKey) {
        this.userId = userId;
        this.serialNumber = serialNumber;
        this.code = code;
        this.encryptedApiKey = encryptedApiKey;
    }

    public String getUserId() {
        return userId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getCode() {
        return code;
    }

    /**
     * Get the encrypted API key.
     * @return Optional with the encrypted API key. Note that the API key will be null if the user has not completed the
     * login process
     */
    public Optional<ByteBuffer> getEncryptedApiKey() {
        return Optional.ofNullable(encryptedApiKey);
    }
}
