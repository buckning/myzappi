package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.User;
import com.amcglynn.myenergi.EddiMode;
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
class SetEddiModeHandlerTest {

    @Mock
    private ZappiService mockZappiService;
    @Mock
    private ZappiService.Builder mockZappiServiceBuilder;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    private IntentRequest intentRequest;

    private SetEddiModeHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SetEddiModeHandler(mockZappiServiceBuilder, mockUserIdResolverFactory);
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("SetEddiMode").build())
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
    @MethodSource("eddiMode")
    void testHandle(String eddiModeStr, EddiMode eddiMode) {
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockZappiService);
        initIntentRequest(eddiModeStr);
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Changing Eddi mode to "
                + eddiMode.name() + ". This may take a few minutes.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Changing Eddi mode to " +
                eddiMode.name() + ". This may take a few minutes.");

        verify(mockZappiService).setEddiMode(eddiMode);
    }

    @ParameterizedTest
    @MethodSource("invalidEddiModes")
    void testHandleReturnsErrorIfEddiModeIsNotRecognised(String eddiMode) {
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockZappiService);
        initIntentRequest(eddiMode);
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Sorry, I don't recognise that mode.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Sorry, I don't recognise that mode.");

        verify(mockZappiService, never()).setEddiMode(any());
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

    private void initIntentRequest(String chargeMode) {
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder()
                        .putSlotsItem("eddiMode", Slot.builder().withValue(chargeMode).build())
                        .withName("eddiMode").build())
                .build();
    }

    private static Stream<Arguments> eddiMode() {
        return Stream.of(
                Arguments.of("Stopped", EddiMode.STOPPED),
                Arguments.of("Stop", EddiMode.STOPPED),
                Arguments.of("STOPPED", EddiMode.STOPPED),
                Arguments.of("stopped", EddiMode.STOPPED),
                Arguments.of("Normal", EddiMode.NORMAL),
                Arguments.of("NORMAL", EddiMode.NORMAL));
    }

    private static Stream<Arguments> invalidEddiModes() {
        return Stream.of(
                Arguments.of("UNKNOWN"));
    }
}
