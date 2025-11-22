package com.amcglynn.myzappi.core.service.reconciler;

import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myzappi.core.model.Action;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.SqsSenderService;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZappiChargeReconcilerTest {

    @Mock
    private MyEnergiService.Builder myEnergiServiceBuilder;
    @Mock
    private MyEnergiService myEnergiService;
    @Mock
    private ZappiService zappiService;
    @Mock
    private ZappiStatusSummary statusSummary;
    @Mock
    private SqsSenderService sqsSenderService;

    private ZappiChargeReconciler reconciler;
    private final UserId userId = UserId.from("user1");
    private final SerialNumber deviceId = SerialNumber.from("12345678");

    @BeforeEach
    void setUp() {
        reconciler = new ZappiChargeReconciler(sqsSenderService);
    }

    @Test
    void getActionTypeReturnsSetChargeMode() {
        assertThat(reconciler.getActionType()).isEqualTo("setChargeMode");
    }

    @Test
    void getCurrentStateThrowsExceptionWhenNotZappiDevice() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setChargeMode", "FAST", deviceId, DeviceClass.EDDI))
                .build();

        assertThrows(IllegalArgumentException.class, () -> reconciler.getCurrentState(request, myEnergiServiceBuilder));
    }

    @Test
    void getCurrentStateReturnsChargeModeWhenZappiDevice() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setChargeMode", "FAST", deviceId, DeviceClass.ZAPPI))
                .build();

        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getZappiService()).thenReturn(Optional.of(zappiService));
        when(zappiService.getStatusSummary(deviceId)).thenReturn(statusSummary);
        when(statusSummary.getChargeMode()).thenReturn(ZappiChargeMode.ECO);

        ZappiChargeMode result = reconciler.getCurrentState(request, myEnergiServiceBuilder);

        assertThat(result).isEqualTo(ZappiChargeMode.ECO);
    }

    @Test
    void parseDesiredStateReturnsZappiChargeMode() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setChargeMode", "FAST", deviceId, DeviceClass.ZAPPI))
                .build();

        ZappiChargeMode result = reconciler.parseDesiredState(request, myEnergiServiceBuilder);

        assertThat(result).isEqualTo(ZappiChargeMode.FAST);
    }

    @Test
    void isInDesiredStateReturnsTrueWhenStatesEqual() {
        assertThat(reconciler.isInDesiredState(ZappiChargeMode.ECO, ZappiChargeMode.ECO, myEnergiServiceBuilder)).isTrue();
    }

    @Test
    void isInDesiredStateReturnsFalseWhenStatesDifferent() {
        assertThat(reconciler.isInDesiredState(ZappiChargeMode.ECO, ZappiChargeMode.FAST, myEnergiServiceBuilder)).isFalse();
    }

    @Test
    void setStateSetsChargeMode() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setChargeMode", "FAST", deviceId, DeviceClass.ZAPPI))
                .build();

        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getZappiService()).thenReturn(Optional.of(zappiService));

        reconciler.setState(request, ZappiChargeMode.FAST, myEnergiServiceBuilder);

        verify(zappiService).setChargeMode(ZappiChargeMode.FAST);
    }

}
