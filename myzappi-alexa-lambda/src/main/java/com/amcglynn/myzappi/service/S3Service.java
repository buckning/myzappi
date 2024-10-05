package com.amcglynn.myzappi.service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Duration;

@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner presigner;

    public S3Service() {
        var region = Region.EU_WEST_1;
        s3Client = S3Client.builder()
                .region(region)
                .build();
        presigner = S3Presigner.builder()
                .region(region)
                .build();
    }

    public URL generatePresignUrl(String bucketName, String keyName, Duration ttl) {
        var getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();

        var presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(ttl)
                .build();

        return presigner.presignGetObject(presignRequest).url();
    }

    public void uploadToS3(String bucketName, String keyName, byte[] imageData, String contentType) {
        var imageInputStream = new ByteArrayInputStream(imageData);

        var objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .contentType(contentType)
                .build();

        s3Client.putObject(objectRequest, RequestBody.fromInputStream(imageInputStream, imageData.length));
    }
}
