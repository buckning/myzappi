package com.amcglynn.myzappi.core.service;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptRequest;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class EncryptionService {

    private final KmsClient kmsClient;
    private final String kmsKeyArn;

    public EncryptionService(String kmsKeyArn) {
        this.kmsKeyArn = kmsKeyArn;
        kmsClient = KmsClient.builder().build();
    }

    public ByteBuffer encrypt(String data) {
        var encryptRequest = EncryptRequest.builder()
                .keyId(kmsKeyArn)
                .plaintext(SdkBytes.fromByteBuffer(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8))))
                .build();
        var encryptResult = kmsClient.encrypt(encryptRequest);
        return encryptResult.ciphertextBlob().asByteBuffer();
    }

    public String decrypt(ByteBuffer encryptedData) {
        var decryptRequest = DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteBuffer(encryptedData))
                .keyId(kmsKeyArn)
                .build();
        var decryptResult = kmsClient.decrypt(decryptRequest);
        return decryptResult.plaintext().asString(StandardCharsets.UTF_8);
    }
}
