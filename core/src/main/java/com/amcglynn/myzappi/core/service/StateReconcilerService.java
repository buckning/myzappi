package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.DeviceStateReconcileRequestsRepository;
import com.amcglynn.myzappi.core.model.Action;
import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.reconciler.ReconcilerRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@AllArgsConstructor
@Slf4j
public class StateReconcilerService {

    private final ReconcilerRegistry reconcilerRegistry;
    private final DeviceStateReconcileRequestsRepository deviceStateReconcileRequestsRepository;
    private final SqsSenderService sqsSenderService;
    private final MyEnergiService.Builder myEnergiService;

    public void pushReconcileRequest(UserId userId, Action action) {
        var requestId = UUID.randomUUID().toString();
        var sqsMessage = StateReconcileRequest.builder()
                .requestId(requestId)
                .userId(userId)
                .attempt(1)
                .action(action)
                .build();

        deviceStateReconcileRequestsRepository.write(
                userId,
                action.getTarget(),
                action.getType(),
                requestId
        );
        sqsSenderService.sendMessage(sqsMessage);
    }

    public void reconcileDeviceState(StateReconcileRequest request) {
        var requestId = deviceStateReconcileRequestsRepository.read(request.getUserId(),
                request.getAction().getTarget(), request.getAction().getType());

        if (requestId.isEmpty() || !requestId.get().equals(request.getRequestId())) {
            // Check if request is stale. The requestId in the DB should match the one being processed.
            // The request goes stale if a newer request has been created since this one was sent to SQS.
            // All unknown requests (the ones in the request but not in the DB) are ignored.
            log.info("Ignoring stale reconcile request for userId={}, deviceId={}, type={}",
                    request.getUserId(), request.getAction().getTarget(), request.getAction().getType());
            return;
        }

        var reconcilerOpt = reconcilerRegistry.getReconciler(request.getAction().getType());
        if (reconcilerOpt.isPresent()) {
            reconcilerOpt.get().reconcile(request, myEnergiService);
        } else {
            log.warn("Unknown reconcile action type={} for userId={}.",
                    request.getAction().getType(),
                    request.getUserId());
        }
    }
}
