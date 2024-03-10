package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.User;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.service.EddiService;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoostEddiHandlerTest {

    @Mock
    private MyEnergiService.Builder mockBuilder;
    @Mock
    private MyEnergiService mockMyEnergiService;
    @Mock
    private EddiService mockService;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    private BoostEddiHandler handler;
    private IntentRequest intentRequest;

    @BeforeEach
    void setUp() {
        when(mockBuilder.build(any())).thenReturn(mockMyEnergiService);
        when(mockMyEnergiService.getEddiServiceOrThrow()).thenReturn(mockService);
        handler = new BoostEddiHandler(mockBuilder, mockUserIdResolverFactory);
        initIntentRequest("Duration", "PT25M");
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isTrue();
    }

    @Test
    void testHandleWithDuration() {
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>OK, I'm boosting your hot water now.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "OK, I'm boosting your hot water now.");
        verify(mockService).boostEddi(Duration.of(25, ChronoUnit.MINUTES));
    }

    @Test
    void testHandleRejectsBoostWhenDurationIsLess1Minute() {
        initIntentRequest("Duration", "PT30S");
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>I could not complete that for you. Please try again with a duration between 1 and 99 minutes.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "I could not complete that for you. Please try again with a duration between 1 and 99 minutes.");
        verify(mockService, never()).boostEddi(any());
    }

    @Test
    void testHandleRejectsBoostWhenDurationIsMoreThan99Minutes() {
        initIntentRequest("Duration", "PT100M");
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>I could not complete that for you. Please try again with a duration between 1 and 99 minutes.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "I could not complete that for you. Please try again with a duration between 1 and 99 minutes.");
        verify(mockService, never()).boostEddi(any());
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
                        .withName("BoostEddi").build())
                .build();
    }
}
