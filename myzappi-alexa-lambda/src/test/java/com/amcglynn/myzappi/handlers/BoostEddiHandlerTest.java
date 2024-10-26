package com.amcglynn.myzappi.handlers;

import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.service.EddiService;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

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
    private TestData testData;

    @BeforeEach
    void setUp() {
        when(mockBuilder.build(any())).thenReturn(mockMyEnergiService);
        when(mockMyEnergiService.getEddiServiceOrThrow()).thenReturn(mockService);
        handler = new BoostEddiHandler(mockBuilder, mockUserIdResolverFactory);
        testData = new TestData("BoostEddi", null, Map.of("Duration", "PT25M"));
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(testData.handlerInput())).isTrue();
    }

    @Test
    void testHandleWithDuration() {
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>OK, I'm boosting your hot water now.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "OK, I'm boosting your hot water now.");
        verify(mockService).boostEddi(Duration.of(25, ChronoUnit.MINUTES));
    }

    @Test
    void testHandleRejectsBoostWhenDurationIsLess1Minute() {
        testData = new TestData("BoostEddi", null, Map.of("Duration", "PT30S"));
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>I could not complete that for you. Please try again with a duration between 1 and 99 minutes.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "I could not complete that for you. Please try again with a duration between 1 and 99 minutes.");
        verify(mockService, never()).boostEddi(any());
    }

    @Test
    void testHandleRejectsBoostWhenDurationIsMoreThan99Minutes() {
        testData = new TestData("BoostEddi", null, Map.of("Duration", "PT100M"));
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>I could not complete that for you. Please try again with a duration between 1 and 99 minutes.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "I could not complete that for you. Please try again with a duration between 1 and 99 minutes.");
        verify(mockService, never()).boostEddi(any());
    }
}
