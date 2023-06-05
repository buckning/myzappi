package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.ui.SimpleCard;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.amcglynn.myzappi.handlers.HandlerTestUtils.handlerInputBuilder;
import static com.amcglynn.myzappi.handlers.HandlerTestUtils.requestEnvelopeBuilder;
import static org.assertj.core.api.Assertions.assertThat;

class FallbackHandlerTest {

    private IntentRequest intentRequest;

    private FallbackHandler handler;

    @BeforeEach
    void setUp() {
        handler = new FallbackHandler();
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("AMAZON.FallbackIntent").build())
                .build();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder(intentRequest).build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
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
                .isEqualTo("<speak>Sorry, I don't know how to handle that. Please try again.</speak>");
    }

    @Test
    void testHandleCardResponse() {
        var response = handler.handle(handlerInputBuilder(intentRequest).build());
        assertThat(response).isPresent();

        assertThat(response.get().getCard()).isInstanceOf(SimpleCard.class);
        var simpleCard = (SimpleCard) response.get().getCard();
        assertThat(simpleCard.getTitle()).isEqualTo("My Zappi");
        assertThat(simpleCard.getContent()).isEqualTo("Sorry, I don't know how to handle that. Please try again.");
    }
}
