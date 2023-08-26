package com.amcglynn.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amcglynn.lwa.LwaClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqsHandler implements RequestHandler<SQSEvent, Void> {

    private Properties properties;

    public SqsHandler() {
        properties = new Properties();
    }

    public Void handleRequest(SQSEvent event, Context context) {
        try {
            if (event.getRecords().isEmpty()) {
                log.info("Received no records, exiting...");
                return null;
            }

            // event needs Alexa URL, LWA user ID and Alexa user ID

            var bodyString = event.getRecords().get(0).getBody();
            var body = new ObjectMapper().readValue(bodyString, new TypeReference<MyZappiSqsEvent>() {
            });

            log.info("Received SQS message {}", bodyString);

            var lwaClient = new LwaClient();

            var token = lwaClient.getMessagingToken(properties.getAlexaClientId(), properties.getAlexaClientSecret());
            log.info("Generated token {}", token.getTokenType());
            lwaClient.postSkillMessage(body.getAlexaBaseUrl(), body.getAlexaUserId(), token.getAccessToken(), new SkillMessage(3600, new MyZappiSqsEvent()));
        } catch (Exception e) {
            log.error("Unexpected error", e);
        }
        return null;
    }
}
