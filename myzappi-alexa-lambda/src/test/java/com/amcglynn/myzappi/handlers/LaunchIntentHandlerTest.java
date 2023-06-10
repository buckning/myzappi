package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amazon.ask.model.ui.SimpleCard;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amcglynn.myzappi.core.Brand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LaunchIntentHandlerTest {

    private LaunchHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LaunchHandler();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        var intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("SetChargeMode").build()).build();
        var requestEnvelope = RequestEnvelope.builder().withRequest(intentRequest).build();
        var handlerInput = HandlerInput.builder().withRequestEnvelope(requestEnvelope).build();
        assertThat(handler.canHandle(handlerInput)).isFalse();
    }

    @Test
    void testHandleSpeechResponse() {
        var response = handler.handle(HandlerInput.builder().withRequestEnvelope(requestEnvelopeBuilder().build()).build());
        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech()).isInstanceOf(SsmlOutputSpeech.class);

        var outputSpeech = (SsmlOutputSpeech) response.get().getOutputSpeech();
        assertThat(outputSpeech.getSsml()).isEqualTo("<speak>Hi, I can change your charge type and provide you energy usage. " +
                "Ask me to start charging or to switch to solar. You can also ask me for an energy summary.</speak>");
    }

    @Test
    void testHandleCardResponse() {
        var response = handler.handle(HandlerInput.builder().withRequestEnvelope(requestEnvelopeBuilder().build()).build());
        assertThat(response).isPresent();

        assertThat(response.get().getCard()).isInstanceOf(SimpleCard.class);
        var simpleCard = (SimpleCard) response.get().getCard();
        assertThat(simpleCard.getTitle()).isEqualTo(Brand.NAME);
        assertThat(simpleCard.getContent()).isEqualTo("I can change your charge type and provide you energy usage. " +
                "Ask me to start charging or to switch to solar. You can also ask me for an energy summary.");
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withRequest(initIntentRequest())
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }

    private LaunchRequest initIntentRequest() {
        return LaunchRequest.builder().build();
    }

    private HandlerInput.Builder handlerInputBuilder() {
        return HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build());
    }
}
