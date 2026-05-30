package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myzappi.core.model.AutomationActionType;
import com.amcglynn.myzappi.core.model.AutomationOperator;
import com.amcglynn.myzappi.core.model.AutomationOptions;
import com.amcglynn.myzappi.core.model.AutomationPredicateType;

import java.util.Arrays;
import java.util.List;

public class AutomationOptionsService {

    public AutomationOptions getOptions() {
        return AutomationOptions.builder()
                .predicates(Arrays.stream(AutomationPredicateType.values())
                        .map(predicateType -> AutomationOptions.PredicateOption.builder()
                                .type(predicateType.name())
                                .valueType("DECIMAL")
                                .requiresTarget(predicateType.requiresTarget())
                                .deviceClass(predicateType.getDeviceClass())
                                .build())
                        .toList())
                .operators(Arrays.stream(AutomationOperator.values()).map(Enum::name).toList())
                .actions(List.of(
                        AutomationOptions.ActionOption.builder()
                                .type(AutomationActionType.SET_CHARGE_MODE.getType())
                                .valueType("ENUM")
                                .deviceClass(AutomationActionType.SET_CHARGE_MODE.getDeviceClass())
                                .allowedValues(List.of("ECO_PLUS", "ECO", "FAST", "STOP"))
                                .build(),
                        AutomationOptions.ActionOption.builder()
                                .type(AutomationActionType.SET_ZAPPI_MGL.getType())
                                .valueType("INTEGER")
                                .deviceClass(AutomationActionType.SET_ZAPPI_MGL.getDeviceClass())
                                .min(1)
                                .max(100)
                                .build(),
                        AutomationOptions.ActionOption.builder()
                                .type(AutomationActionType.SET_EDDI_MODE.getType())
                                .valueType("ENUM")
                                .deviceClass(AutomationActionType.SET_EDDI_MODE.getDeviceClass())
                                .allowedValues(List.of("NORMAL", "STOPPED"))
                                .build(),
                        AutomationOptions.ActionOption.builder()
                                .type(AutomationActionType.SET_LIBBI_ENABLED.getType())
                                .valueType("BOOLEAN")
                                .deviceClass(AutomationActionType.SET_LIBBI_ENABLED.getDeviceClass())
                                .allowedValues(List.of("true", "false"))
                                .build(),
                        AutomationOptions.ActionOption.builder()
                                .type(AutomationActionType.SET_LIBBI_CHARGE_FROM_GRID.getType())
                                .valueType("BOOLEAN")
                                .deviceClass(AutomationActionType.SET_LIBBI_CHARGE_FROM_GRID.getDeviceClass())
                                .allowedValues(List.of("true", "false"))
                                .build(),
                        AutomationOptions.ActionOption.builder()
                                .type(AutomationActionType.SET_LIBBI_CHARGE_TARGET.getType())
                                .valueType("INTEGER")
                                .deviceClass(AutomationActionType.SET_LIBBI_CHARGE_TARGET.getDeviceClass())
                                .min(0)
                                .max(100)
                                .build()))
                .build();
    }
}
