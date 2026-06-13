package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.config.Properties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.UUID;

@Slf4j
public class SqsSenderService {
    private final String awsRegion;
    private final String reconciliationSqsQueueUrl;

    public SqsSenderService(Properties properties) {
        this.awsRegion = properties.getAwsRegion();
        this.reconciliationSqsQueueUrl = properties.getReconciliationSqsQueueUrl();
    }

    public void sendMessage(Object message) {
        try {
            var serializedMessage = new ObjectMapper().writeValueAsString(message);
            var send = SendMessageRequest.builder()
                    .queueUrl(reconciliationSqsQueueUrl)
                    .messageBody(serializedMessage)
                    .messageGroupId("default")
                    .messageDeduplicationId(UUID.randomUUID().toString())
                    .build();

            try (var sqs = SqsClient.builder()
                    .region(Region.of(awsRegion))
                    .build()) {
                var result = sqs.sendMessage(send);
                log.info("SQS message sent. MessageId={} MD5OfBody={}", result.messageId(), result.md5OfMessageBody());
            }
        } catch (JsonProcessingException e) {
            log.error("Could not serialize message={}", message, e);
        }
    }
}
