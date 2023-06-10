package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.ui.SimpleCard;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amcglynn.myzappi.core.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.amcglynn.myzappi.handlers.HandlerTestUtils.handlerInputBuilder;
import static com.amcglynn.myzappi.handlers.HandlerTestUtils.requestEnvelopeBuilder;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LogoutHandlerTest {

    private LogoutHandler handler;
    private IntentRequest intentRequest;
    @Mock
    private LoginService loginService;

    @BeforeEach
    void setUp() {
        handler = new LogoutHandler(loginService);
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("Logout").build())
                .build();
    }

    @Test
    void testCanHandleReturnsTrueForCorrectIntent() {
        assertThat(handler.canHandle(handlerInputBuilder(intentRequest).build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseForIncorrectIntent() {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("Unknown").build())
                .build();
        assertThat(handler.canHandle(handlerInputBuilder(intentRequest).build())).isFalse();
    }

    @Test
    void testHandleSpeechResponse() {
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder(intentRequest).build()).build());
        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech()).isInstanceOf(SsmlOutputSpeech.class);

        var outputSpeech = (SsmlOutputSpeech) response.get().getOutputSpeech();
        assertThat(outputSpeech.getSsml())
                .isEqualTo("<speak>You have been logged out.</speak>");
    }

    @Test
    void testHandleCardResponse() {
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder(intentRequest).build()).build());
        assertThat(response).isPresent();

        assertThat(response.get().getCard()).isInstanceOf(SimpleCard.class);
        var simpleCard = (SimpleCard) response.get().getCard();
        assertThat(simpleCard.getTitle()).isEqualTo("My Zappi");
        assertThat(simpleCard.getContent()).isEqualTo("You have been logged out.");
    }
}
