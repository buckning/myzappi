package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amazon.ask.model.services.ServiceClientFactory;
import com.amazon.ask.model.services.directive.DirectiveServiceClient;
import com.amazon.ask.model.services.directive.SendDirectiveRequest;
import com.amazon.ask.model.services.directive.SpeakDirective;
import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusSummaryHandlerTest {

    @Mock
    private ZappiService.Builder mockZappiServiceBuilder;
    @Mock
    private ZappiService mockZappiService;
    @Mock
    private ServiceClientFactory mockServiceClientFactory;
    @Mock
    private DirectiveServiceClient mockDirectiveServiceClient;
    @Captor
    private ArgumentCaptor<SendDirectiveRequest> mockSendDirectiveRequestCaptor;
    private IntentRequest intentRequest;
    private String userId = "userId";
    private StatusSummaryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StatusSummaryHandler(mockZappiServiceBuilder);
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("StatusSummary").build())
                .build();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder()
                        .withName("SetChargeMode").build())
                .build();
        assertThat(handler.canHandle(handlerInputBuilder().build())).isFalse();
    }

    @Test
    void testHandleSendsProgressiveResponseAndReturnsSummary() {
        when(mockZappiServiceBuilder.build(userId)).thenReturn(mockZappiService);
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 1500L, 1400L,
                        24.3, 1000L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.BOOSTING.ordinal(), EvConnectionStatus.CHARGING.getCode()))));
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Solar generation is 1.5 kilowatts. " +
                "Importing 1.0 kilowatts. Boosting 1.4 kilowatts to your E.V. - Charge mode is Eco+. " +
                "Charge added this session is 24.3 kilowatt hours.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Solar: 1.5kW\n" +
                "Import: 1.0kW\n" +
                "Charge rate: 1.4kW\n" +
                "Charge mode: Eco+\n" +
                "Boost mode: enabled\n" +
                "Charge added: 24.3kWh\n");
        verify(mockZappiService).getStatusSummary();
        verify(mockDirectiveServiceClient).enqueue(mockSendDirectiveRequestCaptor.capture());
        var directiveRequest = mockSendDirectiveRequestCaptor.getValue();
        assertThat(directiveRequest.getDirective()).isNotNull().isInstanceOf(SpeakDirective.class);
        var speakDirective = (SpeakDirective) directiveRequest.getDirective();
        assertThat(speakDirective.getSpeech()).isEqualTo("Sure");
    }

    @Test
    void testHandleSendsProgressiveResponseAndReturnsSummaryWithOnlyRequiredInformation() {
        when(mockZappiServiceBuilder.build(userId)).thenReturn(mockZappiService);
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        0.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.PAUSED.ordinal(), EvConnectionStatus.EV_DISCONNECTED.getCode()))));
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Charge mode is Eco+.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Charge mode: Eco+\n");
    }

    @Test
    void testHandleSaysChargeCompleteWhenItChargeStatusIsComplete() {
        when(mockZappiServiceBuilder.build(userId)).thenReturn(mockZappiService);
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        0.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.COMPLETE.ordinal(), EvConnectionStatus.EV_DISCONNECTED.getCode()))));
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Charge mode is Eco+. Charging session is complete.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Charge mode: Eco+\n" +
                "Charge completed\n");
    }

    @Test
    void testHandleSaysExportRate() {
        when(mockZappiServiceBuilder.build(userId)).thenReturn(mockZappiService);
        when(mockServiceClientFactory.getDirectiveService()).thenReturn(mockDirectiveServiceClient);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        0.0, -3000L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.STARTING.ordinal(), EvConnectionStatus.EV_DISCONNECTED.getCode()))));
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Exporting 3.0 kilowatts. Charge mode is Eco+.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Export: 3.0kW\nCharge mode: Eco+\n");
    }

    private HandlerInput.Builder handlerInputBuilder() {
        return HandlerInput.builder()
                .withServiceClientFactory(mockServiceClientFactory)
                .withRequestEnvelope(requestEnvelopeBuilder().build());
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withRequest(intentRequest)
                .withSession(Session.builder().withUser(User.builder().withUserId(userId).build()).build());
    }
}
