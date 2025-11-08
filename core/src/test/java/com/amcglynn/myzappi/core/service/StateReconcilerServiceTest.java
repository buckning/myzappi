package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.EddiState;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myzappi.core.dal.DeviceStateReconcileRequestsRepository;
import com.amcglynn.myzappi.core.model.Action;
import com.amcglynn.myzappi.core.model.DeviceClass;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.StateReconcileRequest;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.EddiStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StateReconcilerServiceTest {

    @Mock
    private MyEnergiService.Builder myEnergiServiceBuilder;

    @Mock
    private MyEnergiService myEnergiService;

    @Mock
    private ZappiService zappiService;
    @Mock
    private SqsSenderService sqsSenderServiceMock;
    @Mock
    private DeviceStateReconcileRequestsRepository mockDeviceStateReconcileRequestsRepository;
    @Mock
    private EddiService eddiService;
    @Mock
    private EddiStatus eddiStatus;

    @Mock
    private ZappiStatusSummary statusSummary;

    private StateReconcilerService serviceUnderTest;
    private final UserId userId = UserId.from("userId");
    private final SerialNumber deviceId = SerialNumber.from("12345678");

    @BeforeEach
    void setUp() {
        serviceUnderTest = new StateReconcilerService(myEnergiServiceBuilder, sqsSenderServiceMock,
                mockDeviceStateReconcileRequestsRepository);
    }

    @Test
    void reconcileDeviceStateDoesNothingWhenTypeIsNotSetChargeMode() {
        var requestId = "x1";
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId(requestId)
                .action(new Action("somethingElse", null, null, null))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, null, "somethingElse"))
                .thenReturn(Optional.of(requestId));

        serviceUnderTest.reconcileDeviceState(request);

        verifyNoInteractions(myEnergiServiceBuilder, sqsSenderServiceMock);
    }

    @Test
    void reconcileDeviceStateDoesNothingWhenSetChargeModeAndDeviceNotZappi() {
        var requestId = "x2";
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId(requestId)
                .action(new Action("setChargeMode", "ECO_PLUS", deviceId, DeviceClass.EDDI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setChargeMode"))
                .thenReturn(Optional.of(requestId));

        serviceUnderTest.reconcileDeviceState(request);

        verifyNoInteractions(myEnergiServiceBuilder, sqsSenderServiceMock);
    }

    @Test
    void reconcileDeviceStateDoesNotReconcileWhenSetChargeModeIsTheSameAsCurrentMode() {
        var mode = ZappiChargeMode.ECO_PLUS;
        var requestId = "req-1";
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(0)
                .requestId(requestId)
                .action(new Action("setChargeMode", mode.toString(), deviceId, DeviceClass.ZAPPI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setChargeMode"))
                .thenReturn(Optional.of(requestId));
        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getZappiService()).thenReturn(Optional.of(zappiService));
        when(zappiService.getStatusSummary(ArgumentMatchers.any())).thenReturn(statusSummary);
        when(statusSummary.getChargeMode()).thenReturn(mode);

        serviceUnderTest.reconcileDeviceState(request);

        verify(myEnergiServiceBuilder).build(ArgumentMatchers.any());
        verify(myEnergiService).getZappiService();
        verify(zappiService).getStatusSummary(ArgumentMatchers.any());
        verify(statusSummary).getChargeMode();
        verifyNoMoreInteractions(myEnergiServiceBuilder, myEnergiService, zappiService, statusSummary);
    }

    @Test
    void reconcileDeviceStateSetsStateWhenDesiredModeIsNotEqualToCurrentMode() {
        var currentMode = ZappiChargeMode.ECO;
        var desiredMode = ZappiChargeMode.ECO_PLUS;
        var requestId = "req-2";
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId(requestId)
                .action(new Action("setChargeMode", desiredMode.toString(), deviceId, DeviceClass.ZAPPI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setChargeMode"))
                .thenReturn(Optional.of(requestId));
        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getZappiService()).thenReturn(Optional.of(zappiService));
        when(zappiService.getStatusSummary(ArgumentMatchers.any())).thenReturn(statusSummary);
        when(statusSummary.getChargeMode()).thenReturn(currentMode);

        serviceUnderTest.reconcileDeviceState(request);

        verify(myEnergiServiceBuilder).build(ArgumentMatchers.any());
        verify(zappiService).getStatusSummary(ArgumentMatchers.any());
        verify(statusSummary).getChargeMode();
        verify(zappiService).setChargeMode(desiredMode);
        var captor = ArgumentCaptor.forClass(StateReconcileRequest.class);
        verify(sqsSenderServiceMock).sendMessage(captor.capture());
        var sent = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(sent.getAttempt()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(sent.getRequestId()).isEqualTo(requestId);
        verifyNoMoreInteractions(myEnergiServiceBuilder, myEnergiService, zappiService, statusSummary);
    }

    @Test
    void reconcileDeviceState_zappi_differentMode_atMaxRetries_dropsWithoutQueueing() {
        var currentMode = ZappiChargeMode.ECO;
        var desiredMode = ZappiChargeMode.ECO_PLUS;
        var requestId = "req-3";
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(3)
                .requestId(requestId)
                .action(new Action("setChargeMode", desiredMode.toString(), deviceId, DeviceClass.ZAPPI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setChargeMode"))
                .thenReturn(Optional.of(requestId));
        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getZappiService()).thenReturn(Optional.of(zappiService));
        when(zappiService.getStatusSummary(ArgumentMatchers.any())).thenReturn(statusSummary);
        when(statusSummary.getChargeMode()).thenReturn(currentMode);

        serviceUnderTest.reconcileDeviceState(request);

        verify(myEnergiServiceBuilder).build(ArgumentMatchers.any());
        verify(zappiService).getStatusSummary(ArgumentMatchers.any());
        verify(statusSummary).getChargeMode();
        verify(zappiService).setChargeMode(desiredMode);
        verifyNoInteractions(sqsSenderServiceMock);
    }

    @Test
    void reconcileDeviceStateIgnoresStaleRequestWhenRequestIdIsNotSavedInTheDb() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId("some-id")
                .action(new Action("setChargeMode", ZappiChargeMode.FAST.toString(), deviceId, DeviceClass.ZAPPI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setChargeMode"))
                .thenReturn(Optional.of("different-id"));

        serviceUnderTest.reconcileDeviceState(request);

        verifyNoInteractions(myEnergiServiceBuilder, sqsSenderServiceMock);
    }

    @Test
    void reconcileEddiModeSetsModeToStoppedWhenNotInDesiredState() {
        var requestId = "e1";
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId(requestId)
                .action(new Action("setEddiMode", EddiMode.STOPPED.toString(), deviceId, DeviceClass.EDDI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setEddiMode"))
                .thenReturn(Optional.of(requestId));
        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getEddiService()).thenReturn(Optional.of(eddiService));
        when(eddiService.getStatus(deviceId)).thenReturn(eddiStatus);
        when(eddiStatus.getState()).thenReturn(EddiState.BOOST);

        serviceUnderTest.reconcileDeviceState(request);

        verify(myEnergiServiceBuilder).build(ArgumentMatchers.any());
        org.mockito.Mockito.verify(myEnergiService, org.mockito.Mockito.times(2)).getEddiService();
        verify(eddiService).getStatus(deviceId);
        verify(eddiService).setEddiMode(EddiMode.STOPPED);
    }

    @Test
    void reconcileEddiModeSetsModeToNormalWhenNotInDesiredState() {
        var requestId = "e2";
        var desired = EddiMode.NORMAL;
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId(requestId)
                .action(new Action("setEddiMode", desired.toString(), deviceId, DeviceClass.EDDI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setEddiMode"))
                .thenReturn(Optional.of(requestId));
        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getEddiService()).thenReturn(Optional.of(eddiService));
        when(eddiService.getStatus(deviceId)).thenReturn(eddiStatus);
        when(eddiStatus.getState()).thenReturn(EddiState.STOPPED);

        serviceUnderTest.reconcileDeviceState(request);

        verify(myEnergiServiceBuilder).build(ArgumentMatchers.any());
        org.mockito.Mockito.verify(myEnergiService, org.mockito.Mockito.times(2)).getEddiService();
        verify(eddiService).getStatus(deviceId);
        verify(eddiService).setEddiMode(desired);
    }

    @Test
    void reconcileEddiModeIgnoredWhenAlreadyDesiredStoppedState() {
        var requestId = "e3";
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId(requestId)
                .action(new Action("setEddiMode", EddiMode.STOPPED.toString(), deviceId, DeviceClass.EDDI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setEddiMode"))
                .thenReturn(Optional.of(requestId));
        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getEddiService()).thenReturn(Optional.of(eddiService));
        when(eddiService.getStatus(deviceId)).thenReturn(eddiStatus);
        when(eddiStatus.getState()).thenReturn(EddiState.STOPPED);

        serviceUnderTest.reconcileDeviceState(request);

        verify(myEnergiServiceBuilder).build(ArgumentMatchers.any());
        verify(myEnergiService).getEddiService();
        verify(eddiService).getStatus(deviceId);
        verifyNoMoreInteractions(eddiService);
    }

    @Test
    void reconcileEddiModeIgnoredWhenAlreadyDesiredStateNormal() {
        var requestId = "e4";
        var desired = EddiMode.NORMAL;
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(1)
                .requestId(requestId)
                .action(new Action("setEddiMode", desired.toString(), deviceId, DeviceClass.EDDI))
                .build();

        when(mockDeviceStateReconcileRequestsRepository.read(userId, deviceId, "setEddiMode"))
                .thenReturn(Optional.of(requestId));
        when(myEnergiServiceBuilder.build(ArgumentMatchers.any())).thenReturn(myEnergiService);
        when(myEnergiService.getEddiService()).thenReturn(Optional.of(eddiService));
        when(eddiService.getStatus(deviceId)).thenReturn(eddiStatus);
        when(eddiStatus.getState()).thenReturn(EddiState.DIVERTING);

        serviceUnderTest.reconcileDeviceState(request);

        verify(myEnergiServiceBuilder).build(ArgumentMatchers.any());
        verify(myEnergiService).getEddiService();
        verify(eddiService).getStatus(deviceId);
        verifyNoMoreInteractions(eddiService);
    }
}
