package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amcglynn.myenergi.EddiMode;
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
class SetEddiModeToStoppedHandlerTest {

    @Mock
    private MyEnergiService mockMyEnergiService;
    @Mock
    private EddiService mockEddiService;
    @Mock
    private MyEnergiService.Builder mockMyEnergiServiceBuilder;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    private IntentRequest intentRequest;

    private SetEddiModeToStoppedHandler handler;

    @BeforeEach
    void setUp() {
        when(mockMyEnergiService.getEddiServiceOrThrow()).thenReturn(mockEddiService);
        handler = new SetEddiModeToStoppedHandler(mockMyEnergiServiceBuilder, mockUserIdResolverFactory);
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder()
                        .withName("SetEddiModeToStopped").build())
                .withLocale("en-GB")
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

    @Test
    void testHandle() {
        when(mockMyEnergiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Changing Eddi mode to stopped. This may take a few minutes.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Changing Eddi mode to stopped. This may take a few minutes.");

        verify(mockEddiService).setEddiMode(EddiMode.STOPPED);
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
