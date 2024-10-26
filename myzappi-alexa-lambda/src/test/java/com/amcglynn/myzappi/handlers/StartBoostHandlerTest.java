package com.amcglynn.myzappi.handlers;

import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

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
    private MyEnergiService.Builder mockBuilder;
    @Mock
    private MyEnergiService mockMyEnergiService;
    @Mock
    private ZappiService mockService;
    private StartBoostHandler handler;
    private TestData testData;

    @BeforeEach
    void setUp() {
        when(mockMyEnergiService.getZappiServiceOrThrow()).thenReturn(mockService);
        when(mockBuilder.build(any())).thenReturn(mockMyEnergiService);
        handler = new StartBoostHandler();
        testData = new TestData("StartBoostMode", mockService);
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(testData.handlerInput())).isTrue();
    }

    @Test
    void testHandleWithKilowattHours() {
        testData = new TestData("StartBoostMode", mockService, Map.of("KiloWattHours", "20"));

        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Charging 20.0 kilowatt hours</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Charging: 20.0kWh");
        verify(mockService).startBoost(new KiloWattHour(20));
    }
}
