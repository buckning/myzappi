package com.amcglynn.myzappi.handlers;

import com.amazon.ask.model.services.ServiceClientFactory;
import com.amazon.ask.model.services.directive.DirectiveServiceClient;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.model.services.directive.SpeakDirective;
import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class GetSolarReportHandlerTest {
    @Mock
    private ZappiService mockZappiService;
    @Mock
    private ServiceClientFactory mockServiceClientFactory;
    @Mock
    private DirectiveServiceClient mockDirectiveServiceClient;
    @Captor
    private ArgumentCaptor<SendDirectiveRequest> mockSendDirectiveRequestCaptor;
    private GetSolarReportHandler handler;
    private TestData testData;

    @BeforeEach
    void setUp() {
        handler = new GetSolarReportHandler();
        testData = new TestData("GetSolarReport", mockZappiService);
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
    void testHandleSendsProgressiveResponseAndReturnsSolarGeneration() {
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 1500L, 1400L,
                        24.3, 1000L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.BOOSTING.ordinal(), EvConnectionStatus.CHARGING.getCode()))));
        var result = handler.handle(testData.handlerInput(mockServiceClientFactory));
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Solar generation is 1.5 kilowatts.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Solar: 1.5kW\n");
        verify(mockZappiService).getStatusSummary();
        verify(mockDirectiveServiceClient).enqueue(mockSendDirectiveRequestCaptor.capture());
        var directiveRequest = mockSendDirectiveRequestCaptor.getValue();
        assertThat(directiveRequest.getDirective()).isNotNull().isInstanceOf(SpeakDirective.class);
        var speakDirective = (SpeakDirective) directiveRequest.getDirective();
        assertThat(speakDirective.getSpeech()).isEqualTo("Sure");
    }

    @Test
    void testHandleSendsProgressiveResponseAndReturnsSolarGenerationAndRoundsToZeroWhenLessThan100Watts() {
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 99L, 1400L,
                        24.3, 1000L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.BOOSTING.ordinal(), EvConnectionStatus.CHARGING.getCode()))));
        var result = handler.handle(testData.handlerInput(mockServiceClientFactory));
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Solar generation is 0.0 kilowatts.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Solar: 0.0kW\n");
        verify(mockZappiService).getStatusSummary();
        verify(mockDirectiveServiceClient).enqueue(mockSendDirectiveRequestCaptor.capture());
        var directiveRequest = mockSendDirectiveRequestCaptor.getValue();
        assertThat(directiveRequest.getDirective()).isNotNull().isInstanceOf(SpeakDirective.class);
        var speakDirective = (SpeakDirective) directiveRequest.getDirective();
        assertThat(speakDirective.getSpeech()).isEqualTo("Sure");
    }
}
