package com.amcglynn.myzappi.core.service.reconciler;

import com.amcglynn.myzappi.core.model.Action;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.SqsSenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeviceStateReconcilerTest {

    @Mock
    private SqsSenderService sqsSenderService;

    @Mock
    private MyEnergiService.Builder myEnergiServiceBuilder;

    private TestableDeviceStateReconciler reconciler;
    private final UserId userId = UserId.from("testUser");
    private final SerialNumber serialNumber = SerialNumber.from("12345678");

    @BeforeEach
    void setUp() {
        reconciler = new TestableDeviceStateReconciler(sqsSenderService);
    }

    @Test
    void reconcileDropsRequestWhenMaxRetriesReached() {
        var request = createStateReconcileRequest(3); // 3 attempts (max retries)

        reconciler.reconcile(request, myEnergiServiceBuilder);

        assertThat(reconciler.wasGetCurrentStateCalled()).isFalse();
        assertThat(reconciler.wasParseDesiredStateCalled()).isFalse();
        assertThat(reconciler.wasIsInDesiredStateCalled()).isFalse();
        assertThat(reconciler.wasSetStateCalled()).isFalse();
        verify(sqsSenderService, never()).sendMessage(any());
    }

    @Test
    void reconcile_completesSuccessfully_whenStatesMatch() {
        var request = createStateReconcileRequest(1);
        reconciler.setStatesMatch(true);

        reconciler.reconcile(request, myEnergiServiceBuilder);

        assertThat(reconciler.wasGetCurrentStateCalled()).isTrue();
        assertThat(reconciler.wasParseDesiredStateCalled()).isTrue();
        assertThat(reconciler.wasIsInDesiredStateCalled()).isTrue();
        assertThat(reconciler.wasSetStateCalled()).isFalse();
        verify(sqsSenderService, never()).sendMessage(any());
    }

    @Test
    void reconcile_setsStateAndSendsRetry_whenStatesDoNotMatch() {
        var request = createStateReconcileRequest(1);
        reconciler.setStatesMatch(false);

        reconciler.reconcile(request, myEnergiServiceBuilder);

        assertThat(reconciler.wasGetCurrentStateCalled()).isTrue();
        assertThat(reconciler.wasParseDesiredStateCalled()).isTrue();
        assertThat(reconciler.wasIsInDesiredStateCalled()).isTrue();
        assertThat(reconciler.wasSetStateCalled()).isTrue();

        ArgumentCaptor<StateReconcileRequest> retryRequestCaptor = ArgumentCaptor.forClass(StateReconcileRequest.class);
        verify(sqsSenderService).sendMessage(retryRequestCaptor.capture());
        
        StateReconcileRequest retryRequest = retryRequestCaptor.getValue();
        assertThat(retryRequest.getRequestId()).isEqualTo(request.getRequestId());
        assertThat(retryRequest.getUserId()).isEqualTo(request.getUserId());
        assertThat(retryRequest.getAttempt()).isEqualTo(2); // Incremented from 1 to 2
        assertThat(retryRequest.getAction()).isEqualTo(request.getAction());
    }

    @Test
    void reconcile_handlesMultipleRetryAttempts() {
        var request = createStateReconcileRequest(2); // Second attempt
        reconciler.setStatesMatch(false);

        reconciler.reconcile(request, myEnergiServiceBuilder);
        assertThat(reconciler.wasSetStateCalled()).isTrue();

        ArgumentCaptor<StateReconcileRequest> retryRequestCaptor = ArgumentCaptor.forClass(StateReconcileRequest.class);
        verify(sqsSenderService).sendMessage(retryRequestCaptor.capture());
        
        StateReconcileRequest retryRequest = retryRequestCaptor.getValue();
        assertThat(retryRequest.getAttempt()).isEqualTo(3); // Incremented from 2 to 3
    }

    @Test
    void reconcile_passesCorrectParametersToAbstractMethods() {
        var request = createStateReconcileRequest(1);
        reconciler.setStatesMatch(false);

        reconciler.reconcile(request, myEnergiServiceBuilder);

        assertThat(reconciler.getGetCurrentStateRequest()).isEqualTo(request);
        assertThat(reconciler.getGetCurrentStateBuilder()).isEqualTo(myEnergiServiceBuilder);
        assertThat(reconciler.getParseDesiredStateRequest()).isEqualTo(request);
        assertThat(reconciler.getParseDesiredStateBuilder()).isEqualTo(myEnergiServiceBuilder);
        assertThat(reconciler.getIsInDesiredStateCurrent()).isEqualTo("current");
        assertThat(reconciler.getIsInDesiredStateDesired()).isEqualTo("desired");
        assertThat(reconciler.getIsInDesiredStateBuilder()).isEqualTo(myEnergiServiceBuilder);
        assertThat(reconciler.getSetStateRequest()).isEqualTo(request);
        assertThat(reconciler.getSetStateDesired()).isEqualTo("desired");
        assertThat(reconciler.getSetStateBuilder()).isEqualTo(myEnergiServiceBuilder);
    }

    private StateReconcileRequest createStateReconcileRequest(int attempt) {
        return StateReconcileRequest.builder()
                .requestId("req-123")
                .userId(userId)
                .attempt(attempt)
                .action(new Action("testAction", "testValue", serialNumber, DeviceClass.ZAPPI))
                .build();
    }

    /**
     * Testable implementation of DeviceStateReconciler that allows us to control
     * the behavior and verify method calls without testing specific reconciler implementations.
     */
    private static class TestableDeviceStateReconciler extends DeviceStateReconciler<String> {
        private boolean statesMatch = true;
        private boolean getCurrentStateCalled = false;
        private boolean parseDesiredStateCalled = false;
        private boolean isInDesiredStateCalled = false;
        private boolean setStateCalled = false;

        // Captured parameters for verification
        private StateReconcileRequest getCurrentStateRequest;
        private MyEnergiService.Builder getCurrentStateBuilder;
        private StateReconcileRequest parseDesiredStateRequest;
        private MyEnergiService.Builder parseDesiredStateBuilder;
        private String isInDesiredStateCurrent;
        private String isInDesiredStateDesired;
        private MyEnergiService.Builder isInDesiredStateBuilder;
        private StateReconcileRequest setStateRequest;
        private String setStateDesired;
        private MyEnergiService.Builder setStateBuilder;

        public TestableDeviceStateReconciler(SqsSenderService sqsSenderService) {
            super(sqsSenderService);
        }

        @Override
        public String getActionType() {
            return "testAction";
        }

        @Override
        protected String getCurrentState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder) {
            getCurrentStateCalled = true;
            getCurrentStateRequest = request;
            getCurrentStateBuilder = myEnergiServiceBuilder;
            return "current";
        }

        @Override
        protected String parseDesiredState(StateReconcileRequest request, MyEnergiService.Builder myEnergiServiceBuilder) {
            parseDesiredStateCalled = true;
            parseDesiredStateRequest = request;
            parseDesiredStateBuilder = myEnergiServiceBuilder;
            return "desired";
        }

        @Override
        protected boolean isInDesiredState(String current, String desired, MyEnergiService.Builder myEnergiServiceBuilder) {
            isInDesiredStateCalled = true;
            isInDesiredStateCurrent = current;
            isInDesiredStateDesired = desired;
            isInDesiredStateBuilder = myEnergiServiceBuilder;
            return statesMatch;
        }

        @Override
        protected void setState(StateReconcileRequest request, String desired, MyEnergiService.Builder myEnergiServiceBuilder) {
            setStateCalled = true;
            setStateRequest = request;
            setStateDesired = desired;
            setStateBuilder = myEnergiServiceBuilder;
        }

        // Test control methods
        public void setStatesMatch(boolean statesMatch) {
            this.statesMatch = statesMatch;
        }

        // Verification methods
        public boolean wasGetCurrentStateCalled() { return getCurrentStateCalled; }
        public boolean wasParseDesiredStateCalled() { return parseDesiredStateCalled; }
        public boolean wasIsInDesiredStateCalled() { return isInDesiredStateCalled; }
        public boolean wasSetStateCalled() { return setStateCalled; }

        // Parameter getters for verification
        public StateReconcileRequest getGetCurrentStateRequest() { return getCurrentStateRequest; }
        public MyEnergiService.Builder getGetCurrentStateBuilder() { return getCurrentStateBuilder; }
        public StateReconcileRequest getParseDesiredStateRequest() { return parseDesiredStateRequest; }
        public MyEnergiService.Builder getParseDesiredStateBuilder() { return parseDesiredStateBuilder; }
        public String getIsInDesiredStateCurrent() { return isInDesiredStateCurrent; }
        public String getIsInDesiredStateDesired() { return isInDesiredStateDesired; }
        public MyEnergiService.Builder getIsInDesiredStateBuilder() { return isInDesiredStateBuilder; }
        public StateReconcileRequest getSetStateRequest() { return setStateRequest; }
        public String getSetStateDesired() { return setStateDesired; }
        public MyEnergiService.Builder getSetStateBuilder() { return setStateBuilder; }
    }
}
