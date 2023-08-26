package com.amcglynn.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqsHandler implements RequestHandler<SQSEvent, Void> {

    public Void handleRequest(SQSEvent event, Context context) {
        if (event.getRecords().isEmpty()) {
            log.info("Received no records, exiting...");
            return null;
        }

        var dateStr = event.getRecords().get(0).getBody();

        log.info("Received SQS message");



        return null;
    }
}
