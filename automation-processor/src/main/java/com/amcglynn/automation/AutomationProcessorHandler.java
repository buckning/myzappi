package com.amcglynn.automation;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.core.dal.AutomationProcessorLockRepository;
import com.amcglynn.myzappi.core.dal.AutomationRepository;
import com.amcglynn.myzappi.core.service.automation.AutomationProcessorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
public class AutomationProcessorHandler implements RequestHandler<AutomationProcessorEvent, Void> {

    private static final int BATCH_LIMIT = 25;
    private static final int CONTINUATION_SAFETY_MILLIS = 30_000;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final AutomationRepository automationRepository;
    private final AutomationProcessorLockRepository lockRepository;
    private final AutomationProcessorService processorService;
    private final AWSLambda lambdaClient;
    private final String lambdaName;
    private final Supplier<Instant> instantSupplier;
    private final ObjectMapper objectMapper;

    public AutomationProcessorHandler() {
        var serviceManager = new ServiceManager(new Properties());
        this.automationRepository = serviceManager.getAutomationRepository();
        this.lockRepository = serviceManager.getAutomationProcessorLockRepository();
        this.processorService = serviceManager.getAutomationProcessorService();
        this.lambdaClient = AWSLambdaClientBuilder.defaultClient();
        this.lambdaName = serviceManager.getProperties().getAutomationProcessorLambdaName();
        this.instantSupplier = Instant::now;
        this.objectMapper = new ObjectMapper();
    }

    AutomationProcessorHandler(AutomationRepository automationRepository,
                               AutomationProcessorLockRepository lockRepository,
                               AutomationProcessorService processorService,
                               AWSLambda lambdaClient,
                               String lambdaName,
                               Supplier<Instant> instantSupplier) {
        this.automationRepository = automationRepository;
        this.lockRepository = lockRepository;
        this.processorService = processorService;
        this.lambdaClient = lambdaClient;
        this.lambdaName = lambdaName;
        this.instantSupplier = instantSupplier;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Void handleRequest(AutomationProcessorEvent event, Context context) {
        var request = event == null ? new AutomationProcessorEvent() : event;
        var existingRun = request.getRunId() != null;
        var runId = existingRun ? request.getRunId() : UUID.randomUUID().toString();
        var lockExpiresAt = instantSupplier.get().plus(LOCK_DURATION);
        var locked = existingRun ? lockRepository.refresh(runId, lockExpiresAt) : lockRepository.acquire(runId, lockExpiresAt);
        if (!locked) {
            log.info("Automation processor lock is already held; exiting");
            return null;
        }

        var page = automationRepository.scan(request.getLastEvaluatedKey(), BATCH_LIMIT);
        for (var userAutomations : page.getUserAutomations()) {
            if (context.getRemainingTimeInMillis() < CONTINUATION_SAFETY_MILLIS) {
                invokeContinuation(runId, request.getLastEvaluatedKey());
                return null;
            }
            try {
                processorService.processUser(userAutomations.getUserId(), userAutomations.getAutomations());
            } catch (Exception e) {
                log.error("Failed to process automations for user {}", userAutomations.getUserId(), e);
            }
        }

        if (page.hasMore()) {
            invokeContinuation(runId, page.getLastEvaluatedKey());
            return null;
        }

        lockRepository.release(runId);
        return null;
    }

    @SneakyThrows
    private void invokeContinuation(String runId, Map<String, AttributeValue> lastEvaluatedKey) {
        log.info("Automation processor continuation for runId {}", runId);
        var continuation = AutomationProcessorEvent.builder()
                .runId(runId)
                .lastEvaluatedKey(lastEvaluatedKey)
                .build();
        lambdaClient.invoke(new InvokeRequest()
                .withFunctionName(lambdaName)
                .withInvocationType("Event")
                .withPayload(ByteBuffer.wrap(objectMapper.writeValueAsBytes(continuation))));
    }
}
