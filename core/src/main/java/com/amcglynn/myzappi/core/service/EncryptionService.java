package com.amcglynn.myzappi.core.service;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.EncryptRequest;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class EncryptionService {

    private final AWSKMS kmsClient;
    private final String kmsKeyArn;

    public EncryptionService(String kmsKeyArn) {
        this.kmsKeyArn = kmsKeyArn;
        kmsClient = AWSKMSClientBuilder.standard().build();
    }

    public ByteBuffer encrypt(String data) {
        var encryptRequest = new EncryptRequest().withKeyId(kmsKeyArn)
                .withPlaintext(ByteBuffer.wrap(data.getBytes()));
        var encryptResult = kmsClient.encrypt(encryptRequest);
        return encryptResult.getCiphertextBlob();
    }

    public String decrypt(ByteBuffer encryptedData) {
        var decryptRequest = new DecryptRequest()
                .withCiphertextBlob(encryptedData)
                .withKeyId(kmsKeyArn);
        var decryptResult = kmsClient.decrypt(decryptRequest);
        var plaintextKey = decryptResult.getPlaintext();
        return new String(plaintextKey.array(), StandardCharsets.UTF_8);
    }
}
