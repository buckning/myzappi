package com.amcglynn.myzappi.handlers;

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
class UnlockZappiHandlerTest {

    @Mock
    private ZappiService mockZappiService;

    private UnlockZappiHandler handler;
    private TestData testData;

    @BeforeEach
    void setUp() {
        testData = new TestData("UnlockZappi", mockZappiService);
        handler = new UnlockZappiHandler();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(testData.handlerInput())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        assertThat(handler.canHandle(new TestData("Unknown", mockZappiService).handlerInput())).isFalse();
    }

    @Test
    void testHandle() {
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Unlocking your charger. This may take a few minutes.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Unlocking your charger. This may take a few minutes.");
        verify(mockZappiService).unlockZappi();
    }
}
