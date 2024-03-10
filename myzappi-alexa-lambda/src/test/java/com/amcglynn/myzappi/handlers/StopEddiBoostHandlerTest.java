package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
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

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StopEddiBoostHandlerTest {

    @Mock
    private MyEnergiService.Builder mockMyEnergiServiceBuilder;
    @Mock
    private MyEnergiService mockMyEnergiService;
    @Mock
    private EddiService mockEddiService;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;

    private StopEddiBoostHandler handler;
    private IntentRequest intentRequest;

    @BeforeEach
    void setUp() {
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        when(mockMyEnergiService.getEddiServiceOrThrow()).thenReturn(mockEddiService);
        handler = new StopEddiBoostHandler(mockMyEnergiServiceBuilder, mockUserIdResolverFactory);
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder().withName("StopEddiBoost").build())
                .build();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder()
                        .withName("SetChargeMode").build())
                .build();
        assertThat(handler.canHandle(handlerInputBuilder().build())).isFalse();
    }

    @Test
    void testHandle() {
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Stopping hot water boost now.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Stopping hot water boost now.");
        verify(mockEddiService).stopEddiBoost();
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
}
