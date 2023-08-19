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
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StartBoostHandlerTest {

    @Mock
    private ZappiService.Builder mockBuilder;
    @Mock
    private ZappiService mockService;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    @Mock
    private UserZoneResolver mockUserZoneResolver;
    private StartBoostHandler handler;
    private IntentRequest intentRequest;

    @BeforeEach
    void setUp() {
        when(mockBuilder.build(any())).thenReturn(mockService);
        handler = new StartBoostHandler(mockBuilder, mockUserIdResolverFactory, mockUserZoneResolver);
        initIntentRequest("Duration", "PT25M");
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isTrue();
    }

    @Test
    void testHandleWithDuration() {
        when(mockService.startSmartBoost(any(Duration.class))).thenReturn(LocalTime.parse("09:15:00"));

        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Boosting until 9:15 am</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Boosting until 9:15 am.");
        verify(mockService).startSmartBoost(Duration.of(25, ChronoUnit.MINUTES));
    }

    @Test
    void testHandleWithEndTime() {
        when(mockService.startSmartBoost(any(LocalTime.class))).thenReturn(LocalTime.parse("10:30:00"));
        initIntentRequest("Time", "10:30");

        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Boosting until 10:30 am</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Boosting until 10:30 am.");
        verify(mockService).startSmartBoost(LocalTime.of(10, 30));
    }

    @Test
    void testHandleWithKilowattHours() {
        initIntentRequest("KiloWattHours", "20");

        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Charging 20.0 kilowatt hours</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Charging 20.0 kilowatt hours");
        verify(mockService).startBoost(new KiloWattHour(20));
    }

    @Test
    void testHandleWithNoSlotValues() {
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder()
                        .withName("StartBoostMode").build())
                .build();

        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Sorry, I didn't understand that</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Sorry, I didn't understand that");
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

    private void initIntentRequest(String slotName, String slotValue) {
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder()
                        .putSlotsItem(slotName, Slot.builder().withValue(slotValue).build())
                        .withName("StartBoostMode").build())
                .build();
    }
}
