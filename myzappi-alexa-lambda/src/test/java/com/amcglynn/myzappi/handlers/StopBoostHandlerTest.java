package com.amcglynn.myzappi.handlers;

import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StopBoostHandlerTest {
    @Mock
    private ZappiService mockZappiService;

    private StopBoostHandler handler;
    private TestData testData;

    @BeforeEach
    void setUp() {
        testData = new TestData("StopBoostMode", mockZappiService);
        handler = new StopBoostHandler();
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
    void testHandle() {
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Stopping boost mode now.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Stopping boost mode now.");
        verify(mockZappiService).stopBoost();
    }
}
