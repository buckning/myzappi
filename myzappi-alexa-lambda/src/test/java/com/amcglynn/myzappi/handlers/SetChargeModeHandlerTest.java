package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.User;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.stream.Stream;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetChargeModeHandlerTest {

    @Mock
    private ZappiService mockZappiService;
    @Mock
    private ZappiService.Builder mockZappiServiceBuilder;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    private IntentRequest intentRequest;

    private SetChargeModeHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SetChargeModeHandler(mockZappiServiceBuilder, mockUserIdResolverFactory);
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("SetChargeMode").build())
                .build();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("GoGreen").build())
                .build();
        assertThat(handler.canHandle(handlerInputBuilder().build())).isFalse();
    }

    @ParameterizedTest
    @MethodSource("zappiChargeMode")
    void testHandle(ZappiChargeMode zappiChargeMode) {
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockZappiService);
        initIntentRequest(zappiChargeMode);
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Changing charge mode to "
                + zappiChargeMode.getDisplayName() + ". This may take a few minutes.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Changing charge mode to " +
                zappiChargeMode.getDisplayName() + ". This may take a few minutes.");

        verify(mockZappiService).setChargeMode(zappiChargeMode);
    }

    @ParameterizedTest
    @MethodSource("invalidChargeModes")
    void testHandleReturnsErrorIfChargeModeIsNotRecognised(String zappiChargeMode) {
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockZappiService);
        initIntentRequest(zappiChargeMode);
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Sorry, I don't recognise that charge mode.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Sorry, I don't recognise that charge mode.");

        verify(mockZappiService, never()).setChargeMode(any());
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

    private void initIntentRequest(ZappiChargeMode chargeMode) {
        initIntentRequest(chargeMode.getDisplayName());
    }

    private void initIntentRequest(String chargeMode) {
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder()
                        .putSlotsItem("ChargeMode", Slot.builder().withValue(chargeMode).build())
                        .withName("ChargeMode").build())
                .build();
    }

    private static Stream<Arguments> zappiChargeMode() {
        return Stream.of(
                Arguments.of(ZappiChargeMode.STOP),
                Arguments.of(ZappiChargeMode.ECO_PLUS),
                Arguments.of(ZappiChargeMode.ECO),
                Arguments.of(ZappiChargeMode.FAST));
    }

    private static Stream<Arguments> invalidChargeModes() {
        return Stream.of(
                Arguments.of(ZappiChargeMode.BOOST.getDisplayName()),
                Arguments.of("UNKNOWN"));
    }
}
