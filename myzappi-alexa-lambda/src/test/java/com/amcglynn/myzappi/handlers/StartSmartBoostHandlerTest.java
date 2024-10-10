package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.User;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StartSmartBoostHandlerTest {

    @Mock
    private MyEnergiService.Builder mockBuilder;
    @Mock
    private MyEnergiService mockMyEnergiService;
    @Mock
    private ZappiService mockService;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    private StartSmartBoostHandler handler;
    private IntentRequest intentRequest;

    @BeforeEach
    void setUp() {
        when(mockMyEnergiService.getZappiServiceOrThrow()).thenReturn(mockService);
        when(mockBuilder.build(any())).thenReturn(mockMyEnergiService);
        handler = new StartSmartBoostHandler(mockBuilder, mockUserIdResolverFactory);
        initIntentRequest("20", "14:00");
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isTrue();
    }

    @Test
    void testHandle() {
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Starting smart boost with 20.0 kilowatt hours and finish time at 14:00</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Charging: 20.0kWh, finish charging at: 14:00");
        verify(mockService).startSmartBoost(new KiloWattHour(20), LocalTime.of(14, 0));
    }

    @Test
    void testHandleTellsUserThatThereAreValuesMissingFromTheVoiceCommand() {
        initIntentRequest();    // no kilowatt hours or finish time in the slots
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Please ask me to start a smart boost with a specific amount of energy and a finish time.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Please ask me to start a smart boost with a specific amount of energy and a finish time.");
        verify(mockService, never()).startSmartBoost(any(), any());
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

    private void initIntentRequest() {
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder()
                        .withName("StartSmartBoost").build())
                .build();
    }

    private void initIntentRequest(String kiloWattHours, String finishChargingAt) {
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder()
                        .putSlotsItem("KiloWattHours", Slot.builder().withValue(kiloWattHours).build())
                        .putSlotsItem("Time", Slot.builder().withValue(finishChargingAt).build())
                        .withName("StartSmartBoost").build())
                .build();
    }
}
