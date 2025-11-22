package com.amcglynn.myzappi.reconciler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.SqsSenderService;
import com.amcglynn.myzappi.core.service.StateReconcilerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class StateReconcilerHandler implements RequestHandler<SQSEvent, Void> {
    private final ObjectMapper objectMapper;
    private final StateReconcilerService stateReconilerService;

    public StateReconcilerHandler() {
        this.objectMapper = new ObjectMapper();
        var properties = new Properties();
        var serviceManager = new ServiceManager(properties);
        var myenergiServiceBuilder = new MyEnergiService.Builder(serviceManager.getLoginService());
        stateReconilerService = new StateReconcilerService(myenergiServiceBuilder,
                new SqsSenderService(properties), serviceManager.getDeviceStateReconcileRequestsRepository());
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        if (event == null || event.getRecords() == null || event.getRecords().isEmpty()) {
            log.info("StateReconciler invoked with no SQS records, event={}", event);
            return null;
        }

        for (SQSEvent.SQSMessage record : event.getRecords()) {
            log.info("MessageId={} Body={}", record.getMessageId(), record.getBody());

            getStateReconcileRequest(record)
                .ifPresentOrElse(
                    request -> {
                        stateReconilerService.reconcileDeviceState(request);
                    },
                    () -> log.warn("Skipping invalid StateReconcileRequest for MessageId={}", record.getMessageId())
                );
        }
        return null;
    }

    private Optional<StateReconcileRequest> getStateReconcileRequest(SQSEvent.SQSMessage sqsMessage) {
        try {
            return Optional.of(objectMapper.readValue(sqsMessage.getBody(), StateReconcileRequest.class));
        } catch (Exception e) {
            log.error("Failed to parse SQS message body into StateReconcileRequest, MessageId={}", sqsMessage.getMessageId(), e);
            return Optional.empty();
        }
    }
}
