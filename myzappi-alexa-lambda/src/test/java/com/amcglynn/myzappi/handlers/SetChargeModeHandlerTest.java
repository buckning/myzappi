package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.User;
import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myenergi.exception.InvalidResponseFormatException;
import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.google.common.util.concurrent.MoreExecutors;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetChargeModeHandlerTest {

    @Mock
    private ZappiService mockZappiService;

    private SetChargeModeHandler handler;
    private TestData testData;

    @BeforeEach
    void setUp() {
        testData = new TestData("SetChargeMode", mockZappiService);
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        0.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.DIVERTING.ordinal(), EvConnectionStatus.EV_CONNECTED.getCode()))));
        handler = new SetChargeModeHandler(MoreExecutors.newDirectExecutorService());
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(testData.handlerInput())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        assertThat(handler.canHandle(new TestData("Unknown").handlerInput())).isFalse();
    }

    @ParameterizedTest
    @MethodSource("zappiChargeMode")
    void testHandle(ZappiChargeMode zappiChargeMode) {
        testData = new TestData("SetChargeMode", mockZappiService, Map.of("ChargeMode", zappiChargeMode.getDisplayName()));

        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Changing charge mode to "
                + zappiChargeMode.getDisplayName() + ". This may take a few minutes.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Changing charge mode to " +
                zappiChargeMode.getDisplayName() + ". This may take a few minutes.");

        verify(mockZappiService).setChargeMode(zappiChargeMode);
    }

    @Test
    void testGetStatusFailsInSeparateThreadExpectTheChargeModeToStillBeSet() {
        doThrow(new InvalidResponseFormatException()).when(mockZappiService).getStatusSummary();
        testData = new TestData("SetChargeMode", mockZappiService, Map.of("ChargeMode", ZappiChargeMode.ECO_PLUS.getDisplayName()));
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Changing charge mode to "
                + ZappiChargeMode.ECO_PLUS.getDisplayName() + ". This may take a few minutes.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Changing charge mode to " +
                ZappiChargeMode.ECO_PLUS.getDisplayName() + ". This may take a few minutes.");

        verify(mockZappiService).setChargeMode(ZappiChargeMode.ECO_PLUS);
    }

    @ParameterizedTest
    @MethodSource("invalidChargeModes")
    void testHandleReturnsErrorIfChargeModeIsNotRecognised(String zappiChargeMode) {
        testData = new TestData("SetChargeMode", mockZappiService, Map.of("ChargeMode", zappiChargeMode));
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Sorry, I don't recognise that charge mode.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Sorry, I don't recognise that charge mode.");

        verify(mockZappiService, never()).setChargeMode(any());
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
