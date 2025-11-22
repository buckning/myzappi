package com.amcglynn.myzappi.core.service.reconciler;

import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.SqsSenderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract template for device state reconciliation.
 * Defines the common workflow for checking and setting device states.
 * 
 * @param <T> The type of state being reconciled (e.g., ZappiChargeMode, EddiMode)
 */
@AllArgsConstructor
@Slf4j
public abstract class DeviceStateReconciler<T> {

    private static final int DEFAULT_MAX_RETRIES = 3;

    private final SqsSenderService sqsSenderService;

    /**
     * Template method that orchestrates the reconciliation process.
     * This method should not be overridden by subclasses.
     */
    public final void reconcile(StateReconcileRequest request,
            MyEnergiService.Builder myEnergiServiceBuilder) {
        log.info("Starting reconciliation for userId={}, actionType={}, target={}",
                request.getUserId(), request.getAction().getType(), request.getAction().getTarget());

        if (request.getAttempt() >= DEFAULT_MAX_RETRIES) {
            log.info("Dropping reconcile request for userId={}, type={} after {} attempts",
                    request.getUserId(), request.getAction().getType(), request.getAttempt());
            return;
        }
        
        T currentState = getCurrentState(request, myEnergiServiceBuilder);
        T desiredState = parseDesiredState(request, myEnergiServiceBuilder);
        
        log.info("Current state: {}, Desired state: {}", currentState, desiredState);
        
        if (!isInDesiredState(currentState, desiredState, myEnergiServiceBuilder)) {
            log.info("State not in desired configuration, setting new state");
            setState(request, desiredState, myEnergiServiceBuilder);
            sendSqsRetry(request);
        } else {
            log.info("Reconciliation completed for userId={}, actionType={}, target={}",
                    request.getUserId(), request.getAction().getType(), request.getAction().getTarget());
        }
    }

    /**
     * Retrieve the current state from the device.
     */
    protected abstract T getCurrentState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder);

    /**
     * Parse the desired state from the request.
     */
    protected abstract T parseDesiredState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder);

    /**
     * Determine if the current state matches the desired state.
     */
    protected abstract boolean isInDesiredState(T current, T desired, MyEnergiService.Builder myEnergiServiceBuilder);

    /**
     * Set the device to the desired state.
     */
    protected abstract void setState(StateReconcileRequest request, T desired, MyEnergiService.Builder myEnergiServiceBuilder);

    /**
     * Handle post-reconciliation logic with retry support.
     * This method is final and handles the common retry pattern.
     */
    private void sendSqsRetry(StateReconcileRequest request) {
        var retryRequest = StateReconcileRequest.builder()
                .requestId(request.getRequestId())
                .userId(request.getUserId())
                .attempt(request.getAttempt() + 1)
                .action(request.getAction())
                .build();

        sqsSenderService.sendMessage(retryRequest);
        log.info("Queued retry request for userId={}, attempt={}",
                request.getUserId(), retryRequest.getAttempt());
    }

    /**
     * Get the action type this reconciler handles.
     */
    public abstract String getActionType();
}
