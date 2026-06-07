package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.LibbiMode;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.model.Action;
import com.amcglynn.myzappi.core.model.AutomationAction;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.EddiService;
import com.amcglynn.myzappi.core.service.LibbiService;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.StateReconcilerService;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomationActionExecutorTest {

    @Mock
    private StateReconcilerService stateReconcilerService;
    @Mock
    private MyEnergiService myEnergiService;
    @Mock
    private ZappiService zappiService;
    @Mock
    private EddiService eddiService;
    @Mock
    private LibbiService libbiService;
    @Captor
    private ArgumentCaptor<Action> actionCaptor;

    private AutomationActionExecutor executor;
    private final UserId userId = UserId.from("user-1");
    private final SerialNumber target = SerialNumber.from("10000001");

    @BeforeEach
    void setUp() {
        executor = new AutomationActionExecutor(stateReconcilerService);
    }

    @Test
    void executesSetChargeModeAgainstTargetZappi() {
        when(myEnergiService.getZappiServiceOrThrow()).thenReturn(zappiService);

        executor.execute(userId, myEnergiService, action("setChargeMode", "ECO_PLUS"));

        verify(zappiService).setChargeMode(target, ZappiChargeMode.ECO_PLUS);
    }

    @Test
    void executesSetZappiMglAgainstTargetZappi() {
        when(myEnergiService.getZappiServiceOrThrow()).thenReturn(zappiService);

        executor.execute(userId, myEnergiService, action("setZappiMgl", "42"));

        verify(zappiService).setMgl(target, 42);
    }

    @Test
    void executesSetEddiMode() {
        when(myEnergiService.getEddiServiceOrThrow()).thenReturn(eddiService);

        executor.execute(userId, myEnergiService, action("setEddiMode", "NORMAL"));

        verify(eddiService).setEddiMode(EddiMode.NORMAL);
    }

    @Test
    void executesSetLibbiEnabled() {
        when(myEnergiService.getLibbiServiceOrThrow()).thenReturn(libbiService);

        executor.execute(userId, myEnergiService, action("setLibbiEnabled", "true"));

        verify(libbiService).setMode(target, LibbiMode.ON);
    }

    @Test
    void executesSetLibbiChargeFromGrid() {
        when(myEnergiService.getLibbiServiceOrThrow()).thenReturn(libbiService);

        executor.execute(userId, myEnergiService, action("setLibbiChargeFromGrid", "true"));

        verify(libbiService).setChargeFromGrid(userId, target, true);
    }

    @Test
    void executesSetLibbiChargeTarget() {
        when(myEnergiService.getLibbiServiceOrThrow()).thenReturn(libbiService);

        executor.execute(userId, myEnergiService, action("setLibbiChargeTarget", "80"));

        verify(libbiService).setChargeTarget(userId, target, 80);
    }

    @Test
    void enqueuesReconciliationWhenActionTypeHasRegisteredReconciler() {
        when(myEnergiService.getZappiServiceOrThrow()).thenReturn(zappiService);
        when(stateReconcilerService.supportsReconciliation("setChargeMode")).thenReturn(true);

        executor.execute(userId, myEnergiService, action("setChargeMode", "FAST"));

        verify(stateReconcilerService).pushReconcileRequest(org.mockito.Mockito.eq(userId), actionCaptor.capture());
        assertThat(actionCaptor.getValue().getType()).isEqualTo("setChargeMode");
        assertThat(actionCaptor.getValue().getValue()).isEqualTo("FAST");
        assertThat(actionCaptor.getValue().getTarget()).isEqualTo(target);
        assertThat(actionCaptor.getValue().getDeviceClass()).isEqualTo(DeviceClass.ZAPPI);
    }

    @Test
    void doesNotFailActionWhenReconciliationEnqueueFails() {
        when(myEnergiService.getZappiServiceOrThrow()).thenReturn(zappiService);
        when(stateReconcilerService.supportsReconciliation("setChargeMode")).thenReturn(true);
        doThrow(new RuntimeException("sqs failed")).when(stateReconcilerService)
                .pushReconcileRequest(org.mockito.Mockito.eq(userId), org.mockito.Mockito.any(Action.class));

        assertThatNoException().isThrownBy(() -> executor.execute(userId, myEnergiService,
                action("setChargeMode", "FAST")));
    }

    @Test
    void skipsReconciliationWhenActionTypeHasNoRegisteredReconciler() {
        when(myEnergiService.getLibbiServiceOrThrow()).thenReturn(libbiService);
        when(stateReconcilerService.supportsReconciliation("setLibbiEnabled")).thenReturn(false);

        executor.execute(userId, myEnergiService, action("setLibbiEnabled", "false"));

        verify(stateReconcilerService, never()).pushReconcileRequest(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    private AutomationAction action(String type, String value) {
        return AutomationAction.builder()
                .type(type)
                .target(target.toString())
                .value(value)
                .build();
    }
}
