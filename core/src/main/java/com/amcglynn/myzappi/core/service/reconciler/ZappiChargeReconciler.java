package com.amcglynn.myzappi.core.service.reconciler;

import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.SqsSenderService;
import lombok.extern.slf4j.Slf4j;

/**
 * Reconciler for Zappi charge mode changes.
 * Handles the specific logic for retrieving, comparing, and setting Zappi charge modes.
 */
@Slf4j
public class ZappiChargeReconciler extends DeviceStateReconciler<ZappiChargeMode> {

    public ZappiChargeReconciler(
            SqsSenderService sqsSenderService) {
        super(sqsSenderService);
    }

    @Override
    public String getActionType() {
        return "setChargeMode";
    }

    @Override
    protected ZappiChargeMode getCurrentState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder) {
        // Only handle ZAPPI devices
        if (!DeviceClass.ZAPPI.equals(request.getAction().getDeviceClass())) {
            throw new IllegalArgumentException("ZappiChargeReconciler only handles ZAPPI devices");
        }

        var target = request.getAction().getTarget();
        var service = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
        
        return service.getZappiService().get()
                .getStatusSummary(target).getChargeMode();
    }

    @Override
    protected ZappiChargeMode parseDesiredState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder) {
        return ZappiChargeMode.valueOf(request.getAction().getValue());
    }

    @Override
    protected boolean isInDesiredState(ZappiChargeMode current, ZappiChargeMode desired, MyEnergiService.Builder myEnergiServiceBuilder) {
        return current == desired;
    }

    @Override
    protected void setState(StateReconcileRequest request, ZappiChargeMode desired, MyEnergiService.Builder myEnergiServiceBuilder) {
        var service = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
        service.getZappiService().get().setChargeMode(desired);
        
        log.info("Set charge mode to {} for zappi={}, userId={}",
                desired, request.getAction().getTarget(), request.getUserId());
    }
}
