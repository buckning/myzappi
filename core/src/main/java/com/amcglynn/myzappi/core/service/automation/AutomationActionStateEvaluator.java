package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.LibbiMode;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.model.AutomationAction;
import com.amcglynn.myzappi.core.model.AutomationSnapshot;
import com.amcglynn.myzappi.core.model.SerialNumber;

import java.util.Optional;

public class AutomationActionStateEvaluator {

    public Optional<Boolean> isSatisfied(AutomationAction action, AutomationSnapshot snapshot) {
        var target = SerialNumber.from(action.getTarget().orElseThrow());
        return switch (action.getType()) {
            case "setChargeMode" -> snapshot.getZappiChargeMode(target)
                    .map(current -> current == ZappiChargeMode.valueOf(action.getValue()));
            case "setZappiMgl" -> snapshot.getZappiMinimumGreenLevel(target)
                    .map(current -> current.equals(Integer.parseInt(action.getValue())));
            case "setEddiMode" -> snapshot.getEddiMode(target)
                    .map(current -> isDesiredEddiMode(current, EddiMode.valueOf(action.getValue())));
            case "setLibbiEnabled" -> snapshot.getLibbiMode(target)
                    .map(current -> current == desiredLibbiMode(action.getValue()));
            default -> Optional.empty();
        };
    }

    private boolean isDesiredEddiMode(EddiMode current, EddiMode desired) {
        if (desired == EddiMode.STOPPED) {
            return current == EddiMode.STOPPED;
        }
        return current != EddiMode.STOPPED;
    }

    private LibbiMode desiredLibbiMode(String value) {
        return Boolean.parseBoolean(value) ? LibbiMode.ON : LibbiMode.OFF;
    }
}
