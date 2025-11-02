package com.amcglynn.myzappi.reconciler;

import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.dal.DeviceStateReconcileRequestsRepository;
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
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(0)
                .action(new Action("somethingElse", null, null, null))
                .build();

        serviceUnderTest.reconcileDeviceState(request);

        verifyNoInteractions(myEnergiServiceBuilder);
    }

    @Test
    void reconcileDeviceStateDoesNothingWhenSetChargeModeAndDeviceNotZappi() {
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(0)
                .action(new Action("setChargeMode", "ECO_PLUS", deviceId, DeviceClass.EDDI))
                .build();

        serviceUnderTest.reconcileDeviceState(request);

        verifyNoInteractions(myEnergiServiceBuilder);
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
    void reconcileDeviceState_whenSetChargeMode_andZappi_differentMode_triggersReconcilePath() {
        var currentMode = ZappiChargeMode.ECO;
        var desiredMode = ZappiChargeMode.ECO_PLUS;
        var requestId = "req-2";
        var request = StateReconcileRequest.builder()
                .userId(userId)
                .attempt(0)
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
        verifyNoMoreInteractions(myEnergiServiceBuilder, myEnergiService, zappiService, statusSummary);
    }
}
