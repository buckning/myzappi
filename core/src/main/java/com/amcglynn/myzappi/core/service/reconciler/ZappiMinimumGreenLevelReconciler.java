package com.amcglynn.myzappi.core.service.reconciler;

import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.SqsSenderService;
import lombok.extern.slf4j.Slf4j;

/**
 * Reconciler for Zappi minimum green level.
 * https://support.myenergi.com/hc/en-gb/articles/15587880239249-What-is-the-Minimum-Green-Level-MGL
 */
@Slf4j
public class ZappiMinimumGreenLevelReconciler extends DeviceStateReconciler<Integer> {

    public ZappiMinimumGreenLevelReconciler(
            SqsSenderService sqsSenderService) {
        super(sqsSenderService);
    }

    @Override
    public String getActionType() {
        return "setZappiMgl";
    }

    @Override
    protected Integer getCurrentState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder) {
        // Only handle ZAPPI devices
        if (!DeviceClass.ZAPPI.equals(request.getAction().getDeviceClass())) {
            throw new IllegalArgumentException("ZappiMinimumGreenLevelReconciler only handles ZAPPI devices");
        }

        var target = request.getAction().getTarget();
        var service = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
        
        return service.getZappiService().get()
                .getStatusSummary(target).getMgl();
    }

    @Override
    protected Integer parseDesiredState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder) {
        return Integer.parseInt(request.getAction().getValue());
    }

    @Override
    protected boolean isInDesiredState(Integer current, Integer desired, MyEnergiService.Builder myEnergiServiceBuilder) {
        return current.equals(desired);
    }

    @Override
    protected void setState(StateReconcileRequest request, Integer desired, MyEnergiService.Builder myEnergiServiceBuilder) {
        var service = myEnergiServiceBuilder.build(() -> request.getUserId().toString());
        service.getZappiService().get().setMgl(request.getAction().getTarget(), desired);
        
        log.info("Set MGL to {} for zappi={}, userId={}",
                desired, request.getAction().getTarget(), request.getUserId());
    }
}
