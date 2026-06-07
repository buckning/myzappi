package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.LibbiMode;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.exception.AutomationValidationException;
import com.amcglynn.myzappi.core.model.Action;
import com.amcglynn.myzappi.core.model.AutomationAction;
import com.amcglynn.myzappi.core.model.AutomationActionType;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.StateReconcilerService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AutomationActionExecutor {
    private final StateReconcilerService stateReconcilerService;

    public AutomationActionExecutor(StateReconcilerService stateReconcilerService) {
        this.stateReconcilerService = stateReconcilerService;
    }

    public void execute(UserId userId, MyEnergiService myEnergiService, AutomationAction action) {
        log.info("AutomationActionExecutor executing {} on device {} with value {} for user {}",
                action.getType(), action.getTarget(), action.getValue(), userId);
        var target = SerialNumber.from(action.getTarget().orElseThrow());
        switch (action.getType()) {
            case "setChargeMode" -> myEnergiService.getZappiServiceOrThrow()
                    .setChargeMode(target, ZappiChargeMode.valueOf(action.getValue()));
            case "setZappiMgl" -> myEnergiService.getZappiServiceOrThrow()
                    .setMgl(target, Integer.parseInt(action.getValue()));
            case "setEddiMode" -> myEnergiService.getEddiServiceOrThrow()
                    .setEddiMode(EddiMode.valueOf(action.getValue()));
            case "setLibbiEnabled" -> myEnergiService.getLibbiServiceOrThrow()
                    .setMode(target, Boolean.parseBoolean(action.getValue()) ? LibbiMode.ON : LibbiMode.OFF);
            case "setLibbiChargeFromGrid" -> myEnergiService.getLibbiServiceOrThrow()
                    .setChargeFromGrid(userId, target, Boolean.parseBoolean(action.getValue()));
            case "setLibbiChargeTarget" -> myEnergiService.getLibbiServiceOrThrow()
                    .setChargeTarget(userId, target, Integer.parseInt(action.getValue()));
            default -> throw new AutomationValidationException("Unsupported automation action " + action.getType());
        }
        enqueueReconciliationWhenSupported(userId, action, target);
    }

    private void enqueueReconciliationWhenSupported(UserId userId, AutomationAction action, SerialNumber target) {
        if (!stateReconcilerService.supportsReconciliation(action.getType())) {
            return;
        }
        try {
            var actionType = AutomationActionType.from(action.getType()).orElseThrow();
            stateReconcilerService.pushReconcileRequest(userId,
                    new Action(action.getType(), action.getValue(), target, actionType.getDeviceClass()));
        } catch (Exception e) {
            log.error("Failed to enqueue automation reconciliation for user {}, action {}, target {}",
                    userId, action.getType(), target, e);
        }
    }
}
