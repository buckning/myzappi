package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetPlugStatusHandlerTest {

    @Mock
    private ZappiService.Builder mockZappiServiceBuilder;
    @Mock
    private ZappiService mockZappiService;

    private GetPlugStatusHandler handler;
    private IntentRequest intentRequest;

    @BeforeEach
    void setUp() {
        when(mockZappiServiceBuilder.build(anyString())).thenReturn(mockZappiService);
        handler = new GetPlugStatusHandler(mockZappiServiceBuilder);
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("GetPlugStatus").build())
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
    void testHandleReturnsIfEvIsNotConnected() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        0.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.PAUSED.ordinal(), EvConnectionStatus.EV_DISCONNECTED.getCode()))));
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Your E.V. is not connected.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Your E.V. is not connected.\n");
    }

    @Test
    void testHandleReturnsIfEvIsConnectedAndCharging() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 1000L,
                        0.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.PAUSED.ordinal(), EvConnectionStatus.CHARGING.getCode()))));
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Your E.V. is connected. Charge rate is 1.0 kilowatts.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Your E.V. is connected.\n" +
                "Charge rate is 1.0kW.");
    }

    @Test
    void testHandleReturnsIfEvHasFinishedCharging() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        25.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.COMPLETE.ordinal(), EvConnectionStatus.WAITING_FOR_EV.getCode()))));
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Your E.V. is finished charging. 25.0 kilowatt hours added this session.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Your E.V. is finished charging. 25.0kWh added this session.\n");
    }

    private HandlerInput.Builder handlerInputBuilder() {
        return HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build());
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withRequest(intentRequest)
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }
}