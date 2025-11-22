package com.amcglynn.myzappi.core.service.reconciler;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.EddiState;
import com.amcglynn.myzappi.core.model.Action;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.EddiStatus;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.EddiService;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.SqsSenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EddiModeReconcilerTest {

    @Mock
    private MyEnergiService.Builder myEnergiServiceBuilder;
    @Mock
    private MyEnergiService myEnergiService;
    @Mock
    private EddiService eddiService;
    @Mock
    private EddiStatus eddiStatus;
    @Mock
    private SqsSenderService sqsSenderService;

    private EddiModeReconciler reconciler;
    private final UserId userId = UserId.from("user1");
    private final SerialNumber deviceId = SerialNumber.from("12345678");

    @BeforeEach
    void setUp() {
        reconciler = new EddiModeReconciler(sqsSenderService);
    }

    @Test
    void getActionTypeReturnsSetEddiMode() {
        assertThat(reconciler.getActionType()).isEqualTo("setEddiMode");
    }

    @Test
    void getCurrentStateReturnsModeFromEddiStatus() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setEddiMode", "STOPPED", deviceId, DeviceClass.EDDI))
                .build();

        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getEddiService()).thenReturn(Optional.of(eddiService));
        when(eddiService.getStatus(deviceId)).thenReturn(eddiStatus);
        when(eddiStatus.getState()).thenReturn(EddiState.BOOST);

        EddiMode result = reconciler.getCurrentState(request, myEnergiServiceBuilder);

        assertThat(result).isEqualTo(EddiMode.NORMAL); // BOOST converts to NORMAL
    }

    @Test
    void getCurrentStateReturnsStoppedModeWhenEddiStateStopped() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setEddiMode", "STOPPED", deviceId, DeviceClass.EDDI))
                .build();

        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getEddiService()).thenReturn(Optional.of(eddiService));
        when(eddiService.getStatus(deviceId)).thenReturn(eddiStatus);
        when(eddiStatus.getState()).thenReturn(EddiState.STOPPED);

        EddiMode result = reconciler.getCurrentState(request, myEnergiServiceBuilder);

        assertThat(result).isEqualTo(EddiMode.STOPPED);
    }

    @Test
    void parseDesiredStateReturnsEddiMode() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setEddiMode", "NORMAL", deviceId, DeviceClass.EDDI))
                .build();

        EddiMode result = reconciler.parseDesiredState(request, myEnergiServiceBuilder);

        assertThat(result).isEqualTo(EddiMode.NORMAL);
    }

    @Test
    void isInDesiredStateReturnsTrueWhenStoppedDesiredAndCurrentlyStopped() {
        assertThat(reconciler.isInDesiredState(EddiMode.STOPPED, EddiMode.STOPPED, myEnergiServiceBuilder)).isTrue();
    }

    @Test
    void isInDesiredStateReturnsFalseWhenStoppedDesiredButCurrentlyNormal() {
        assertThat(reconciler.isInDesiredState(EddiMode.NORMAL, EddiMode.STOPPED, myEnergiServiceBuilder)).isFalse();
    }

    @Test
    void isInDesiredStateReturnsTrueWhenNormalDesiredAndCurrentlyNormal() {
        assertThat(reconciler.isInDesiredState(EddiMode.NORMAL, EddiMode.NORMAL, myEnergiServiceBuilder)).isTrue();
    }

    @Test
    void isInDesiredStateReturnsFalseWhenNormalDesiredButCurrentlyStopped() {
        assertThat(reconciler.isInDesiredState(EddiMode.STOPPED, EddiMode.NORMAL, myEnergiServiceBuilder)).isFalse();
    }

    @Test
    void setStateSetsEddiMode() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .action(new Action("setEddiMode", "STOPPED", deviceId, DeviceClass.EDDI))
                .build();

        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getEddiService()).thenReturn(Optional.of(eddiService));

        reconciler.setState(request, EddiMode.STOPPED, myEnergiServiceBuilder);

        verify(eddiService).setEddiMode(EddiMode.STOPPED);
    }

}
