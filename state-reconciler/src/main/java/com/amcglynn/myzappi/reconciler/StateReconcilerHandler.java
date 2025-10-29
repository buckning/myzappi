package com.amcglynn.myzappi.reconciler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateReconcilerHandler implements RequestHandler<SQSEvent, Void> {
    private static final Logger log = LoggerFactory.getLogger(StateReconcilerHandler.class);

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        log.info("StateReconciler invoked");
        if (event == null || event.getRecords() == null || event.getRecords().isEmpty()) {
            log.info("StateReconciler invoked with no SQS records, event={}", event);
            return null;
        }

        log.info("StateReconciler received {} SQS record(s)", event.getRecords().size());
        for (SQSEvent.SQSMessage record : event.getRecords()) {
            log.info("MessageId={} Body={}", record.getMessageId(), record.getBody());
        }
        return null;
    }
}
