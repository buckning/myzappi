package com.amcglynn.myzappi.handlers;

import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class GetChargeRateHandlerTest {
    @Mock
    private ZappiService mockZappiService;
    private GetChargeRateHandler handler;
    private TestData testData;
    private ExecutorService executorService;
    @BeforeEach
    void setUp() {
        handler = new GetChargeRateHandler();
        testData = new TestData("GetChargeRate", mockZappiService);
        executorService = MoreExecutors.newDirectExecutorService();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(testData.handlerInput())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        assertThat(handler.canHandle(new TestData("SetChargeMode").handlerInput())).isFalse();
    }

    @Test
    void testHandleSendsProgressiveResponseAndReturnsSummaryForChargeMode() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 1500L, 1400L,
                        24.3, 1000L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.DIVERTING.ordinal(), EvConnectionStatus.CHARGING.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture), null));

        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Charge rate is 1.4 kilowatts.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Charge rate: 1.4kW");
    }

    @Test
    void testHandleSaysNotChargingWhenItChargeStatusIsComplete() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        0.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.DIVERTING.ordinal(), EvConnectionStatus.EV_CONNECTED.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture), null));

        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Your E.V. is not charging.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Your E.V. is not charging.");
    }

    @Test
    void testHandleSaysChargeCompleteWhenItChargeStatusIsComplete() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        0.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.COMPLETE.ordinal(), EvConnectionStatus.EV_DISCONNECTED.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture), null));

        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Charging session is complete.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Charge completed");
    }
}
