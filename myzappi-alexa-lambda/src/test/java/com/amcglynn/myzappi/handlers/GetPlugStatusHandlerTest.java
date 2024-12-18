package com.amcglynn.myzappi.handlers;

import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.LockStatus;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetPlugStatusHandlerTest {

    @Mock
    private MyEnergiService.Builder mockMyEnergiServiceBuilder;
    @Mock
    private MyEnergiService mockMyEnergiService;
    @Mock
    private ZappiService mockZappiService;

    private GetPlugStatusHandler handler;
    private TestData testData;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        testData = new TestData("GetPlugStatus", mockZappiService);
        when(mockMyEnergiService.getZappiServiceOrThrow()).thenReturn(mockZappiService);
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        handler = new GetPlugStatusHandler();
        executorService = MoreExecutors.newDirectExecutorService();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(testData.handlerInput())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        assertThat(handler.canHandle(new TestData("Unknown").handlerInput())).isFalse();
    }

    @Test
    void testHandleReturnsIfEvIsNotConnected() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        0.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.PAUSED.ordinal(), EvConnectionStatus.EV_DISCONNECTED.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture), null));

        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Your E.V. is not connected.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Your E.V. is not connected.\n");
    }

    @Test
    void testHandleReturnsIfEvIsConnectedAndCharging() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 1000L,
                        2.3, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.PAUSED.ordinal(), EvConnectionStatus.CHARGING.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture), null));

        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Your E.V. is connected. Charge mode is Eco+. " +
                "2.3 kilowatt hours added this session. Charge rate is 1.0 kilowatts.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", """
                Your E.V. is connected.
                Charge mode: Eco+
                Charge added: 2.3kWh
                Charge rate: 1.0kW""");
    }

    @Test
    void testHandleReturnsIfEvHasFinishedCharging() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        25.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.COMPLETE.ordinal(), EvConnectionStatus.WAITING_FOR_EV.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture), null));

        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Your E.V. is finished charging. Charge mode is Eco+. 25.0 kilowatt hours added this session.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", """
                Your E.V. is finished charging.
                Charge mode: Eco+
                Charge added: 25.0kWh
                """);
    }

    @Test
    void testHandleNotifiesIfTheChargerIsLockedButTheCarIsPluggedIn() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        25.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.PAUSED.ordinal(), EvConnectionStatus.EV_CONNECTED.getCode(), LockStatus.LOCKED.getCode(), "v1.2.3"))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture), null));

        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Your E.V. is connected but your charger is locked. It needs to be unlocked before you can start charging.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Your E.V. is connected but your charger is locked. It needs to be unlocked before you can start charging.\n");
    }
}
