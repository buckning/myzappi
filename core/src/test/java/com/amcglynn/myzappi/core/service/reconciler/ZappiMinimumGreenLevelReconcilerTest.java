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
class ZappiMinimumGreenLevelReconcilerTest {

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

    private ZappiMinimumGreenLevelReconciler reconciler;
    private final UserId userId = UserId.from("user1");
    private final SerialNumber deviceId = SerialNumber.from("12345678");

    @BeforeEach
    void setUp() {
        reconciler = new ZappiMinimumGreenLevelReconciler(sqsSenderService);
    }

    @Test
    void getActionTypeReturnsSetZappiMgl() {
        assertThat(reconciler.getActionType()).isEqualTo("setZappiMgl");
    }

    @Test
    void getCurrentStateThrowsExceptionWhenNotZappiDevice() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setZappiMgl", "6", deviceId, DeviceClass.EDDI))
                .build();

        assertThrows(IllegalArgumentException.class, () -> reconciler.getCurrentState(request, myEnergiServiceBuilder));
    }

    @Test
    void getCurrentStateReturnsMglWhenZappiDevice() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setZappiMgl", "6", deviceId, DeviceClass.ZAPPI))
                .build();

        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getZappiService()).thenReturn(Optional.of(zappiService));
        when(zappiService.getStatusSummary(deviceId)).thenReturn(statusSummary);
        when(statusSummary.getMgl()).thenReturn(6);

        var result = reconciler.getCurrentState(request, myEnergiServiceBuilder);

        assertThat(result).isEqualTo(6);
    }

    @Test
    void parseDesiredStateReturnsZappiMgl() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setZappiMgl", "10", deviceId, DeviceClass.ZAPPI))
                .build();

        var result = reconciler.parseDesiredState(request, myEnergiServiceBuilder);

        assertThat(result).isEqualTo(10);
    }

    @Test
    void isInDesiredStateReturnsTrueWhenStatesEqual() {
        assertThat(reconciler.isInDesiredState(100, 100, myEnergiServiceBuilder)).isTrue();
    }

    @Test
    void isInDesiredStateReturnsFalseWhenStatesDifferent() {
        assertThat(reconciler.isInDesiredState(100, 20, myEnergiServiceBuilder)).isFalse();
    }

    @Test
    void setStateSetsMgl() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setZappiMgl", "100", deviceId, DeviceClass.ZAPPI))
                .build();

        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getZappiService()).thenReturn(Optional.of(zappiService));

        reconciler.setState(request, 100, myEnergiServiceBuilder);

        verify(zappiService).setMgl(deviceId, 100);
    }
}
