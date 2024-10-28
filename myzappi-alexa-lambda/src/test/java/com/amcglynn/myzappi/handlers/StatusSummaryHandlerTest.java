package com.amcglynn.myzappi.handlers;

import com.amazon.ask.model.services.ServiceClientFactory;
import com.amazon.ask.model.services.directive.DirectiveServiceClient;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.model.services.directive.SpeakDirective;
import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class StatusSummaryHandlerTest {
    @Mock
    private ZappiService mockZappiService;
    @Mock
    private ServiceClientFactory mockServiceClientFactory;
    @Mock
    private DirectiveServiceClient mockDirectiveServiceClient;
    @Captor
    private ArgumentCaptor<SendDirectiveRequest> mockSendDirectiveRequestCaptor;
    private StatusSummaryHandler handler;
    private TestData testData;
    private ExecutorService executorService;
    private Future<List<ZappiHistory>> historyFuture;

    @BeforeEach
    void setUp() {
        testData = new TestData("StatusSummary", mockZappiService);
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        handler = new StatusSummaryHandler();
        executorService = MoreExecutors.newDirectExecutorService();
        when(mockZappiService.getHistory(any(), any())).thenReturn(List.of());
        historyFuture = executorService.submit(() -> mockZappiService.getHistory(null, null));
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(testData.handlerInput(mockServiceClientFactory))).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        assertThat(handler.canHandle(new TestData("Unknown").handlerInput())).isFalse();
    }

    @Test
    void testHandleSendsProgressiveResponseAndReturnsSummary() {
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 1500L, 1400L,
                        24.3, 1000L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.BOOSTING.ordinal(), EvConnectionStatus.CHARGING.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture,
                "zappiHistory", historyFuture), mockServiceClientFactory));
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Solar generation is 1.5 kilowatts. " +
                "Importing 1.0 kilowatts. Boosting 1.4 kilowatts to your E.V. - Charge mode is Eco+. " +
                "24.3 kilowatt hours added this session.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", """
                Solar: 1.5kW
                Import: 1.0kW
                Charge rate: 1.4kW
                Charge mode: Eco+
                Boost mode: enabled
                Charge added: 24.3kWh
                """);
        verify(mockZappiService).getStatusSummary();
        verify(mockDirectiveServiceClient).enqueue(mockSendDirectiveRequestCaptor.capture());
        var directiveRequest = mockSendDirectiveRequestCaptor.getValue();
        assertThat(directiveRequest.getDirective()).isNotNull().isInstanceOf(SpeakDirective.class);
        var speakDirective = (SpeakDirective) directiveRequest.getDirective();
        assertThat(speakDirective.getSpeech()).isEqualTo("Sure");
    }

    @Test
    void testHandleSendsProgressiveResponseAndReturnsSummaryForChargeMode() {
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 1500L, 1400L,
                        24.3, 1000L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.DIVERTING.ordinal(), EvConnectionStatus.CHARGING.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture,
                "zappiHistory", historyFuture), mockServiceClientFactory));

        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Solar generation is 1.5 kilowatts. " +
                "Importing 1.0 kilowatts. Charge rate is 1.4 kilowatts. Charge mode is Eco+. " +
                "24.3 kilowatt hours added this session.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", """
                Solar: 1.5kW
                Import: 1.0kW
                Charge rate: 1.4kW
                Charge mode: Eco+
                Charge added: 24.3kWh
                """);
    }

    @Test
    void testHandleSendsProgressiveResponseAndReturnsSummaryWithOnlyRequiredInformation() {
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        0.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.PAUSED.ordinal(), EvConnectionStatus.EV_DISCONNECTED.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture,
                "zappiHistory", historyFuture), mockServiceClientFactory));
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Charge mode is Eco+.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Charge mode: Eco+\n");
    }

    @Test
    void testHandleSaysChargeCompleteWhenItChargeStatusIsComplete() {
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        0.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.COMPLETE.ordinal(), EvConnectionStatus.EV_DISCONNECTED.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture,
                "zappiHistory", historyFuture), mockServiceClientFactory));
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Charge mode is Eco+. Charging session is complete.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", """
                Charge mode: Eco+
                Charge completed
                """);
    }

    @Test
    void testHandleSaysExportRate() {
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        0.0, -3000L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.STARTING.ordinal(), EvConnectionStatus.EV_DISCONNECTED.getCode()))));
        var statusSummaryFuture = executorService.submit(() -> mockZappiService.getStatusSummary());

        var result = handler.handle(testData.handlerInput(Map.of("zappiStatusSummary", statusSummaryFuture,
                "zappiHistory", historyFuture), mockServiceClientFactory));
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Exporting 3.0 kilowatts. Charge mode is Eco+.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Export: 3.0kW\nCharge mode: Eco+\n");
    }
}
