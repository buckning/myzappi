package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.config.Properties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;

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
            var sqs = AmazonSQSClientBuilder.standard()
                    .withRegion(awsRegion)
                    .build();

            var send = new SendMessageRequest()
                    .withQueueUrl(reconciliationSqsQueueUrl)
                    .withMessageBody(serializedMessage)
                    .withMessageGroupId("default")
                    .withMessageDeduplicationId(UUID.randomUUID().toString());

            var result = sqs.sendMessage(send);
            log.info("SQS message sent. MessageId={} MD5OfBody={}", result.getMessageId(), result.getMD5OfMessageBody());
        } catch (JsonProcessingException e) {
            log.error("Could not serialize message={}", message, e);
        }
    }
}
