package com.amcglynn.myzappi.handlers;

import com.amcglynn.myenergi.ZappiChargeMode;
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
class GoGreenHandlerTest {

    @Mock
    private ZappiService mockZappiService;
    private GoGreenHandler handler;

    private TestData testData;

    @BeforeEach
    void setUp() {
        testData = new TestData("GoGreen", mockZappiService);
        handler = new GoGreenHandler();
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
    void testHandleSetsChargeModeToEcoPlus() {
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Changing charge mode to Eco+. This may take a few minutes.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Changing charge mode to Eco+. " +
                "This may take a few minutes.");

        verify(mockZappiService).setChargeMode(ZappiChargeMode.ECO_PLUS);
    }
}
