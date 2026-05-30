package com.amcglynn.automation;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amcglynn.myzappi.core.dal.AutomationProcessorLockRepository;
import com.amcglynn.myzappi.core.dal.AutomationRepository;
import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.AutomationScanPage;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.automation.AutomationProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomationProcessorHandlerTest {

    @Mock
    private AutomationRepository automationRepository;
    @Mock
    private AutomationProcessorLockRepository lockRepository;
    @Mock
    private AutomationProcessorService processorService;
    @Mock
    private AWSLambda lambdaClient;
    @Mock
    private Context context;
    @Captor
    private ArgumentCaptor<InvokeRequest> invokeRequestCaptor;

    private AutomationProcessorHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AutomationProcessorHandler(automationRepository, lockRepository, processorService,
                lambdaClient, "automation-processor", () -> Instant.ofEpochSecond(1000));
        lenient().when(context.getRemainingTimeInMillis()).thenReturn(300_000);
    }

    @Test
    void exitsWhenLockCannotBeAcquired() {
        when(lockRepository.acquire(any(), any())).thenReturn(false);

        handler.handleRequest(new AutomationProcessorEvent(), context);

        verify(automationRepository, never()).scan(any(), any(Integer.class));
    }

    @Test
    void scansOneBatchAndProcessesEachUser() {
        when(lockRepository.acquire(any(), any())).thenReturn(true);
        var userAutomations = userAutomations("user-1");
        when(automationRepository.scan(null, 25)).thenReturn(AutomationScanPage.builder()
                .userAutomations(List.of(userAutomations))
                .build());

        handler.handleRequest(new AutomationProcessorEvent(), context);

        verify(processorService).processUser(UserId.from("user-1"), userAutomations.getAutomations());
    }

    @Test
    void invokesContinuationWhenScanHasMoreUsers() {
        when(lockRepository.acquire(any(), any())).thenReturn(true);
        when(automationRepository.scan(null, 25)).thenReturn(AutomationScanPage.builder()
                .userAutomations(List.of(userAutomations("user-1")))
                .lastEvaluatedKey(Map.of("user-id", new AttributeValue("user-1")))
                .build());

        handler.handleRequest(new AutomationProcessorEvent(), context);

        verify(lambdaClient).invoke(invokeRequestCaptor.capture());
        assertThat(invokeRequestCaptor.getValue().getFunctionName()).isEqualTo("automation-processor");
        assertThat(invokeRequestCaptor.getValue().getInvocationType()).isEqualTo("Event");
    }

    @Test
    void invokesContinuationWhenRemainingLambdaTimeIsBelowSafetyThreshold() {
        when(context.getRemainingTimeInMillis()).thenReturn(1_000);
        when(lockRepository.acquire(any(), any())).thenReturn(true);
        when(automationRepository.scan(null, 25)).thenReturn(AutomationScanPage.builder()
                .userAutomations(List.of(userAutomations("user-1")))
                .build());

        handler.handleRequest(new AutomationProcessorEvent(), context);

        verify(lambdaClient).invoke(any(InvokeRequest.class));
        verify(processorService, never()).processUser(any(), any());
    }

    @Test
    void releasesLockAfterFinalBatchCompletes() {
        when(lockRepository.acquire(any(), any())).thenReturn(true);
        when(automationRepository.scan(null, 25)).thenReturn(AutomationScanPage.builder()
                .userAutomations(List.of())
                .build());

        handler.handleRequest(new AutomationProcessorEvent(), context);

        verify(lockRepository).release(any());
    }

    @Test
    void keepsLockWhenContinuationIsInvoked() {
        when(lockRepository.acquire(any(), any())).thenReturn(true);
        when(automationRepository.scan(null, 25)).thenReturn(AutomationScanPage.builder()
                .userAutomations(List.of())
                .lastEvaluatedKey(Map.of("user-id", new AttributeValue("user-1")))
                .build());

        handler.handleRequest(new AutomationProcessorEvent(), context);

        verify(lockRepository, never()).release(any());
    }

    @Test
    void malformedUserRowDoesNotStopOtherUsersInBatch() {
        when(lockRepository.acquire(any(), any())).thenReturn(true);
        var first = userAutomations("user-1");
        var second = userAutomations("user-2");
        when(automationRepository.scan(null, 25)).thenReturn(AutomationScanPage.builder()
                .userAutomations(List.of(first, second))
                .build());
        org.mockito.Mockito.doThrow(new RuntimeException("bad row")).when(processorService)
                .processUser(eq(UserId.from("user-1")), any());

        handler.handleRequest(new AutomationProcessorEvent(), context);

        verify(processorService).processUser(eq(UserId.from("user-2")), any());
    }

    private AutomationScanPage.UserAutomations userAutomations(String userId) {
        return AutomationScanPage.UserAutomations.builder()
                .userId(UserId.from(userId))
                .automations(List.of(Automation.builder().automationId("automation-1").build()))
                .build();
    }
}
