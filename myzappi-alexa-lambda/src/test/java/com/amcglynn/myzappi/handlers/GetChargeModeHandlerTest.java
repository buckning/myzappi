package com.amcglynn.myzappi.handlers;

import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
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

import java.util.List;
import java.util.Map;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class GetChargeModeHandlerTest {

    @Mock
    private ZappiService mockZappiService;
    private GetChargeModeHandler handler;
    private TestData testData;

    @BeforeEach
    void setUp() {
        handler = new GetChargeModeHandler();
        testData = new TestData("GetChargeMode", mockZappiService);
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(testData.handlerInput())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        assertThat(handler.canHandle(new TestData("SetChargeMode", mockZappiService).handlerInput())).isFalse();
    }

    @Test
    void testHandleSendsProgressiveResponseAndReturnsSummary() {
        var executorService = MoreExecutors.newDirectExecutorService();
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 1500L, 1400L,
                        24.3, 1000L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.BOOSTING.ordinal(), EvConnectionStatus.CHARGING.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture), null));
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Charge mode is Eco+.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Charge mode: Eco+");
        verify(mockZappiService).getStatusSummary();
    }
}
