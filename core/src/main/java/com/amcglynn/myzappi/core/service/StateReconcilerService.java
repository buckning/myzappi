package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.EddiState;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.dal.DeviceStateReconcileRequestsRepository;
import com.amcglynn.myzappi.core.model.Action;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.model.UserId;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@AllArgsConstructor
@Slf4j
public class StateReconcilerService {

    private final MyEnergiService.Builder myenergiServiceBuilder;
    private final SqsSenderService sqsSenderService;
    private final DeviceStateReconcileRequestsRepository deviceStateReconcileRequestsRepository;
    private static final int MAX_RETRIES = 3;

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

    // action, stateSupplier, isInDesiredState, setState
    public void reconcileDeviceState(StateReconcileRequest request) {
        var requestId = deviceStateReconcileRequestsRepository.read(request.getUserId(),
                request.getAction().getTarget(), request.getAction().getType());

        if (requestId.isEmpty() || !requestId.get().equals(request.getRequestId())) {
            log.info("Ignoring stale reconcile request for userId={}, deviceId={}, type={}",
                    request.getUserId(), request.getAction().getTarget(), request.getAction().getType());
            return;
        }

        if (request.getAction().getType().equals("setChargeMode")) {
            reconcileZappiChargeMode(request);
        } else if (request.getAction().getType().equals("setEddiMode")) {
            reconcileEddiMode(request);
        } else {
            log.warn("Unknown reconcile action type={} for userId={}", request.getAction().getType(), request.getUserId());
        }
    }

    private void reconcileEddiMode(StateReconcileRequest request) {
        var target = request.getAction().getTarget();
        var service = myenergiServiceBuilder.build(() -> request.getUserId().toString());
        var eddiStatus = service.getEddiService().get()
                .getStatus(target);
        var currentState = eddiStatus.getState().toString();
        var desiredState = request.getAction().getValue();

        if (!isDesiredEddiMode(currentState, desiredState)) {
            log.info("Eddi mode not in desired state for userId={}, eddi={}, current={}, desired={}. Reconciling",
                    request.getUserId(), target, currentState, desiredState);
            service.getEddiService().get()
                    .setEddiMode(EddiMode.valueOf(desiredState));

            if (request.getAttempt() == MAX_RETRIES) {
                log.info("Dropping reconcile request for userId={}, type={} after {} attempts",
                        request.getUserId(), request.getAction().getType(), request.getAttempt());
                return;
            }

            // queue up another SQS message to set the charge mode but increment the attempt count
            sqsSenderService.sendMessage(
                    StateReconcileRequest.builder()
                            .requestId(request.getRequestId())
                            .userId(request.getUserId())
                            .attempt(request.getAttempt() + 1)
                            .action(request.getAction())
                            .build());
        } else {
            log.info("Eddi mode in desired state for userId={}, eddi={}, current={}, desired={}. No action required",
                    request.getUserId(), target, currentState, desiredState);
        }
    }

    boolean isDesiredEddiMode(String currentState, String desiredMode) {
        if (desiredMode.equals(EddiMode.STOPPED.toString())) {
            return currentState.equals(EddiState.STOPPED.toString());
        }
        return !currentState.equals(EddiState.STOPPED.toString());
    }

    private void reconcileZappiChargeMode(StateReconcileRequest request) {
        var retryRequired = false;
        if (DeviceClass.ZAPPI.equals(request.getAction().getDeviceClass())) {
            var target = request.getAction().getTarget();
            var service = myenergiServiceBuilder.build(() -> request.getUserId().toString());
            var currentChargeMode = service.getZappiService().get()
                    .getStatusSummary(target).getChargeMode();

            var desiredChargeMode = ZappiChargeMode.valueOf(request.getAction().getValue());

            if (!(currentChargeMode == desiredChargeMode)) {
                log.info("Charge mode not in desired state for userId={}, zappi={}, current={}, desired={}. Reconciling",
                        request.getUserId(), target, currentChargeMode, desiredChargeMode);
                service.getZappiService().get().setChargeMode(desiredChargeMode);
                retryRequired = true;
            }
        }

        if (request.getAttempt() == MAX_RETRIES) {
            log.info("Dropping reconcile request for userId={}, type={} after {} attempts",
                    request.getUserId(), request.getAction().getType(), request.getAttempt());
            return;
        }

        if (retryRequired) {
            // queue up another SQS message to set the charge mode but increment the attempt count
            sqsSenderService.sendMessage(
                    StateReconcileRequest.builder()
                            .requestId(request.getRequestId())
                            .userId(request.getUserId())
                            .attempt(request.getAttempt() + 1)
                            .action(request.getAction())
                            .build());
        }
    }
}
