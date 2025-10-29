package com.amcglynn.myzappi.api.service;

import com.amcglynn.myzappi.api.rest.Request;
import com.amcglynn.myzappi.api.rest.Response;
import com.amcglynn.myzappi.core.config.Properties;
import lombok.extern.slf4j.Slf4j;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import java.util.UUID;

@Slf4j
public class SqsSenderService {

    private final Properties properties;

    public SqsSenderService(Properties properties) {
        this.properties = properties;
    }

    public Response sendTestMessage(Request request) {
        log.info("Sending message to SQS in region: {}", properties.getAwsRegion());
        AmazonSQS sqs = AmazonSQSClientBuilder.standard()
                .withRegion(properties.getAwsRegion())
                .build();


        String queueUrl = properties.getSqsQueueUrl();
        log.info("Sending message to SQS queue at URL: {}", queueUrl);

        String message = "hello world";
        SendMessageRequest send = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(message)
                .withMessageGroupId("default")
                .withMessageDeduplicationId(UUID.randomUUID().toString());

        var result = sqs.sendMessage(send);
        log.info("SQS message sent. MessageId={} MD5OfBody={}", result.getMessageId(), result.getMD5OfMessageBody());
        return new Response(204);
    }
}
