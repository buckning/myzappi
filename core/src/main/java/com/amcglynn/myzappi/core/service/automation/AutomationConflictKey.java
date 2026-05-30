package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.SerialNumber;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class AutomationConflictKey {
    private final String actionType;
    private final SerialNumber target;

    public static AutomationConflictKey from(Automation automation) {
        return new AutomationConflictKey(automation.getAction().getType(),
                SerialNumber.from(automation.getAction().getTarget().orElseThrow()));
    }
}
