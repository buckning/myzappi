package com.amcglynn.myzappi.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class AutomationOptions {
    private List<PredicateOption> predicates;
    private List<String> operators;
    private List<ActionOption> actions;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class PredicateOption {
        private String type;
        private String valueType;
        private boolean requiresTarget;
        private DeviceClass deviceClass;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class ActionOption {
        private String type;
        private String valueType;
        private DeviceClass deviceClass;
        private List<String> allowedValues;
        private Integer min;
        private Integer max;
    }
}
