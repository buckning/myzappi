package com.amcglynn.myzappi.handlers;

import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.util.Map;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StartSmartBoostHandlerTest {

    @Mock
    private ZappiService mockService;
    private StartSmartBoostHandler handler;
    private TestData testData;

    @BeforeEach
    void setUp() {
        testData = new TestData("StartSmartBoost", mockService, Map.of("KiloWattHours", "20", "Time", "14:00"));
        handler = new StartSmartBoostHandler();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(testData.handlerInput())).isTrue();
    }

    @Test
    void testHandle() {
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Starting smart boost with 20.0 kilowatt hours and finish time at 14:00</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Charging: 20.0kWh, finish charging at: 14:00");
        verify(mockService).startSmartBoost(new KiloWattHour(20), LocalTime.of(14, 0));
    }

    @Test
    void testHandleTellsUserThatThereAreValuesMissingFromTheVoiceCommand() {
        testData = new TestData("StartSmartBoost", mockService, Map.of());
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Please ask me to start a smart boost with a specific amount of energy and a finish time.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Please ask me to start a smart boost with a specific amount of energy and a finish time.");
        verify(mockService, never()).startSmartBoost(any(), any());
    }
}
