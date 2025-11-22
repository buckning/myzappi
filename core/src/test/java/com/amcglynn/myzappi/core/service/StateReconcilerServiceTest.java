package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.DeviceStateReconcileRequestsRepository;
import com.amcglynn.myzappi.core.model.Action;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.reconciler.DeviceStateReconciler;
import com.amcglynn.myzappi.core.service.reconciler.ReconcilerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StateReconcilerServiceTest {

    @Mock
    private ReconcilerRegistry reconcilerRegistry;
    
    @Mock
    private DeviceStateReconcileRequestsRepository mockDeviceStateReconcileRequestsRepository;
    
    @Mock
    private SqsSenderService sqsSenderServiceMock;
    
    private TestReconciler testReconciler;
    @Mock
    private MyEnergiService.Builder myEnergiServiceBuilder;

    private StateReconcilerService serviceUnderTest;
    private final UserId userId = UserId.from("userId");
    private final SerialNumber deviceId = SerialNumber.from("12345678");

    @BeforeEach
    void setUp() {
        testReconciler = new TestReconciler(sqsSenderServiceMock);
        serviceUnderTest = new StateReconcilerService(reconcilerRegistry,
                mockDeviceStateReconcileRequestsRepository, sqsSenderServiceMock, myEnergiServiceBuilder);
    }

    @Test
    void reconcileDeviceState_delegatesToReconciler_whenReconcilerExists() {
        var requestId = "req-1";
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId(requestId)
                .action(new Action("setChargeMode", "FAST", deviceId, DeviceClass.ZAPPI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setChargeMode"))
                .thenReturn(Optional.of(requestId));
        when(reconcilerRegistry.getReconciler("setChargeMode"))
                .thenReturn(Optional.of(testReconciler));

        serviceUnderTest.reconcileDeviceState(request);

        // Verify the reconciler was called by checking our test reconciler's flag
        assert testReconciler.wasReconcileCalled();
    }

    @Test
    void reconcileDeviceState_ignoresStaleRequest_whenRequestIdMismatch() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId("some-id")
                .action(new Action("setChargeMode", "FAST", deviceId, DeviceClass.ZAPPI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setChargeMode"))
                .thenReturn(Optional.of("different-id"));

        serviceUnderTest.reconcileDeviceState(request);

        verifyNoInteractions(reconcilerRegistry);
    }

    @Test
    void reconcileDeviceState_ignoresStaleRequest_whenRequestIdNotFound() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId("some-id")
                .action(new Action("setChargeMode", "FAST", deviceId, DeviceClass.ZAPPI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setChargeMode"))
                .thenReturn(Optional.empty());

        serviceUnderTest.reconcileDeviceState(request);

        verifyNoInteractions(reconcilerRegistry);
    }

    @Test
    void reconcileDeviceState_handlesUnknownActionType_whenNoReconcilerFound() {
        var requestId = "req-1";
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId(requestId)
                .action(new Action("unknownAction", "value", deviceId, DeviceClass.ZAPPI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "unknownAction"))
                .thenReturn(Optional.of(requestId));
        when(reconcilerRegistry.getReconciler("unknownAction"))
                .thenReturn(Optional.empty());
        when(reconcilerRegistry.getSupportedActionTypes())
                .thenReturn(Set.of("setChargeMode", "setEddiMode"));

        serviceUnderTest.reconcileDeviceState(request);

        verify(reconcilerRegistry).getReconciler("unknownAction");
        verify(reconcilerRegistry).getSupportedActionTypes();
        // No interactions with reconciler since it wasn't found
    }

    /**
     * Test implementation of DeviceStateReconciler for testing purposes
     */
    private static class TestReconciler extends DeviceStateReconciler<String> {
        private boolean reconcileCalled = false;

        public TestReconciler(SqsSenderService sqsSenderService) {
            super(sqsSenderService);
        }

        @Override
        public String getActionType() {
            return "setChargeMode";
        }

        @Override
        protected String getCurrentState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder) {
            reconcileCalled = true; // Track that reconcile was called by tracking when getCurrentState is called
            return "current";
        }

        @Override
        protected String parseDesiredState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder) {
            return "desired";
        }

        @Override
        protected boolean isInDesiredState(String current, String desired, MyEnergiService.Builder myEnergiServiceBuilder) {
            return true; // Always in desired state to avoid triggering setState
        }

        @Override
        protected void setState(StateReconcileRequest request, String desired, MyEnergiService.Builder myEnergiServiceBuilder) {
            // Do nothing
        }

        public boolean wasReconcileCalled() {
            return reconcileCalled;
        }
    }
}
