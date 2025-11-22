package com.amcglynn.myzappi.core.service.reconciler;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.EddiState;
import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.SqsSenderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reconciler for Eddi mode changes.
 * Handles the specific logic for retrieving, comparing, and setting Eddi modes.
 */
@Slf4j
public class EddiModeReconciler extends DeviceStateReconciler<EddiMode> {

    public EddiModeReconciler(
            SqsSenderService sqsSenderService) {
        super(sqsSenderService);
    }

    @Override
    public String getActionType() {
        return "setEddiMode";
    }

    @Override
    protected EddiMode getCurrentState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder) {
        var target = request.getAction().getTarget();
        var service = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
        var eddiStatus = service.getEddiService().get().getStatus(target);
        
        // Convert EddiState to EddiMode for comparison
        return convertStateToMode(eddiStatus.getState());
    }

    @Override
    protected EddiMode parseDesiredState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder) {
        return EddiMode.valueOf(request.getAction().getValue());
    }

    @Override
    protected boolean isInDesiredState(EddiMode current, EddiMode desired, MyEnergiService.Builder myEnergiServiceBuilder) {
        return isDesiredEddiMode(current, desired);
    }

    @Override
    protected void setState(StateReconcileRequest request, EddiMode desired, MyEnergiService.Builder myEnergiServiceBuilder) {
        var service = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
        service.getEddiService().get().setEddiMode(desired);
        
        log.info("Set Eddi mode to {} for eddi={}, userId={}",
                desired, request.getAction().getTarget(), request.getUserId());
    }

    // EddiModeReconciler uses default behavior:
    // - shouldRetry() returns false (no retries needed)
    // - getSqsSenderService() returns null
    // - Parent class handles logging completion

    /**
     * Convert EddiState to EddiMode for comparison purposes.
     * This bridges the gap between the state we read and the mode we want to set.
     */
    private EddiMode convertStateToMode(EddiState state) {
        if (state == EddiState.STOPPED) {
            return EddiMode.STOPPED;
        } else {
            return EddiMode.NORMAL;
        }
    }

    /**
     * Custom logic for determining if Eddi is in desired mode.
     * This preserves the original isDesiredEddiMode logic from StateReconcilerService.
     */
    private boolean isDesiredEddiMode(EddiMode currentMode, EddiMode desiredMode) {
        if (desiredMode == EddiMode.STOPPED) {
            return currentMode == EddiMode.STOPPED;
        }
        return currentMode != EddiMode.STOPPED;
    }
}
