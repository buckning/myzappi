package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myzappi.core.exception.AutomationValidationException;
import com.amcglynn.myzappi.core.exception.CapacityReachedException;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.AutomationAction;
import com.amcglynn.myzappi.core.model.AutomationActionType;
import com.amcglynn.myzappi.core.model.AutomationPredicate;
import com.amcglynn.myzappi.core.model.AutomationPredicateType;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.LoginService;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AutomationValidator {

    private static final int MAX_AUTOMATIONS = 10;
    private static final int MAX_NAME_LENGTH = 80;

    private final LoginService loginService;

    public AutomationValidator(LoginService loginService) {
        this.loginService = loginService;
    }

    public void validateForCreate(UserId userId, Automation automation, int existingCount) {
        if (existingCount >= MAX_AUTOMATIONS) {
            throw new CapacityReachedException("User has reached the maximum number of automations");
        }
        if (automation == null || automation.getPredicate() == null || automation.getAction() == null) {
            throw new AutomationValidationException("Automation predicate and action are required");
        }
        validateName(automation.getName());
        var devices = loginService.readDevices(userId);
        validatePredicate(devices, automation.getPredicate());
        validateAction(devices, automation.getAction());
    }

    public void validatePriorityReorder(List<Automation> existingAutomations, List<String> orderedAutomationIds) {
        if (orderedAutomationIds == null || orderedAutomationIds.size() != existingAutomations.size()) {
            throw new AutomationValidationException("Automation priority reorder must include every automation id once");
        }
        var expectedIds = new HashSet<String>();
        existingAutomations.forEach(automation -> expectedIds.add(automation.getAutomationId()));
        var requestedIds = new HashSet<>(orderedAutomationIds);
        if (requestedIds.size() != orderedAutomationIds.size() || !requestedIds.equals(expectedIds)) {
            throw new AutomationValidationException("Automation priority reorder must include every automation id once");
        }
    }

    private void validateName(String name) {
        if (name != null && name.trim().length() > MAX_NAME_LENGTH) {
            throw new AutomationValidationException("Automation name must be 80 characters or fewer");
        }
    }

    private void validatePredicate(List<MyEnergiDevice> devices, AutomationPredicate predicate) {
        if (isBlank(predicate.getType()) || predicate.getOperator() == null || isBlank(predicate.getValue())) {
            throw new AutomationValidationException("Automation predicate type, operator and value are required");
        }
        var predicateType = parsePredicateType(predicate.getType());
        parseDecimal(predicate.getValue());
        if (!predicateType.requiresTarget()) {
            if (predicate.getTarget().map(target -> !target.isBlank()).orElse(false)) {
                throw new AutomationValidationException("Account-level automation predicates must not define a target");
            }
            return;
        }
        var target = predicate.getTarget()
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new AutomationValidationException("Automation predicate target is required"));
        validateOwnedDevice(devices, target, predicateType.getDeviceClass());
    }

    private void validateAction(List<MyEnergiDevice> devices, AutomationAction action) {
        if (isBlank(action.getType()) || isBlank(action.getValue())) {
            throw new AutomationValidationException("Automation action type and value are required");
        }
        var actionType = AutomationActionType.from(action.getType())
                .orElseThrow(() -> new AutomationValidationException("Unsupported automation action type " + action.getType()));
        var target = action.getTarget()
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new AutomationValidationException("Automation action target is required"));
        validateOwnedDevice(devices, target, actionType.getDeviceClass());
        validateActionValue(action);
    }

    private AutomationPredicateType parsePredicateType(String type) {
        try {
            return AutomationPredicateType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new AutomationValidationException("Unsupported automation predicate type " + type);
        }
    }

    private void parseDecimal(String value) {
        try {
            new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new AutomationValidationException("Automation predicate value must be decimal");
        }
    }

    private void validateOwnedDevice(List<MyEnergiDevice> devices, String target, DeviceClass deviceClass) {
        var serialNumber = SerialNumber.from(target);
        devices.stream()
                .filter(device -> device.getSerialNumber().equals(serialNumber))
                .filter(device -> device.getDeviceClass().equals(deviceClass))
                .findFirst()
                .orElseThrow(() -> new MissingDeviceException(
                        "Target device class is incorrect or user does not own requested device"));
    }

    private void validateActionValue(AutomationAction action) {
        switch (action.getType()) {
            case "setChargeMode" -> requireOneOf(action.getValue(), Set.of("ECO_PLUS", "ECO", "FAST", "STOP"));
            case "setZappiMgl" -> requireIntegerRange(action.getValue(), 1, 100);
            case "setEddiMode" -> requireOneOf(action.getValue(), Set.of("NORMAL", "STOPPED"));
            case "setLibbiEnabled", "setLibbiChargeFromGrid" -> requireOneOf(action.getValue(), Set.of("true", "false"));
            case "setLibbiChargeTarget" -> requireIntegerRange(action.getValue(), 0, 100);
            default -> throw new AutomationValidationException("Unsupported automation action type " + action.getType());
        }
    }

    private void requireOneOf(String value, Set<String> allowedValues) {
        if (!allowedValues.contains(value)) {
            throw new AutomationValidationException("Unsupported automation action value " + value);
        }
    }

    private void requireIntegerRange(String value, int min, int max) {
        try {
            var intValue = Integer.parseInt(value);
            if (intValue < min || intValue > max) {
                throw new AutomationValidationException("Automation action value is outside the supported range");
            }
        } catch (NumberFormatException e) {
            throw new AutomationValidationException("Automation action value must be an integer");
        }
    }

    private boolean isBlank(String value) {
        return Optional.ofNullable(value).map(String::isBlank).orElse(true);
    }
}
