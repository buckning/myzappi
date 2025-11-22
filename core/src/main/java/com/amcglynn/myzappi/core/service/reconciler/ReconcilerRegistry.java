package com.amcglynn.myzappi.core.service.reconciler;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for managing device state reconcilers.
 * Maps action types to their corresponding reconciler implementations.
 */
@Slf4j
public class ReconcilerRegistry {
    
    private final Map<String, DeviceStateReconciler<?>> reconcilers;

    public ReconcilerRegistry(List<DeviceStateReconciler<?>> reconcilerList) {
        this.reconcilers = new HashMap<>();
        reconcilerList.forEach(this::registerReconciler);
    }

    /**
     * Register a reconciler for its action type.
     */
    public void registerReconciler(DeviceStateReconciler<?> reconciler) {
        String actionType = reconciler.getActionType();
        if (reconcilers.containsKey(actionType)) {
            log.warn("Overriding existing reconciler for action type: {}", actionType);
        }
        reconcilers.put(actionType, reconciler);
        log.info("Registered reconciler for action type: {}", actionType);
    }

    /**
     * Get a reconciler for the specified action type.
     * 
     * @param actionType the action type to find a reconciler for
     * @return the reconciler if found, empty otherwise
     */
    public Optional<DeviceStateReconciler<?>> getReconciler(String actionType) {
        return Optional.ofNullable(reconcilers.get(actionType));
    }

    /**
     * Check if a reconciler exists for the given action type.
     */
    public boolean hasReconciler(String actionType) {
        return reconcilers.containsKey(actionType);
    }

    /**
     * Get all registered action types.
     */
    public java.util.Set<String> getSupportedActionTypes() {
        return reconcilers.keySet();
    }
}
