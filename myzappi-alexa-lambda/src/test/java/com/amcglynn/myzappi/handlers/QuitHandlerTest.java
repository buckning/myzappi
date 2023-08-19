package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Request;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amazon.ask.model.ui.SimpleCard;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amcglynn.myzappi.core.Brand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class QuitHandlerTest {

    private QuitHandler handler;

    @BeforeEach
    void setUp() {
        handler = new QuitHandler();
    }

    @ParameterizedTest
    @MethodSource("quitIntents")
    void testCanHandleOnlyTriggersForTheIntent(String intentName) {
        var intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder().withName(intentName).build()).build();
        var requestEnvelope = RequestEnvelope.builder().withRequest(intentRequest).build();
        var handlerInput = HandlerInput.builder().withRequestEnvelope(requestEnvelope).build();
        assertThat(handler.canHandle(handlerInput)).isTrue();
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
        assertThat(response.get().getShouldEndSession()).isTrue();
        var outputSpeech = (SsmlOutputSpeech) response.get().getOutputSpeech();
        assertThat(outputSpeech.getSsml()).isEqualTo("<speak>Thank you for using My Zappi</speak>");
    }

    @Test
    void testHandleCardResponse() {
        var response = handler.handle(HandlerInput.builder().withRequestEnvelope(requestEnvelopeBuilder().build()).build());
        assertThat(response).isPresent();

        assertThat(response.get().getCard()).isInstanceOf(SimpleCard.class);
        var simpleCard = (SimpleCard) response.get().getCard();
        assertThat(simpleCard.getTitle()).isEqualTo(Brand.NAME);
        assertThat(simpleCard.getContent()).isEqualTo("Thank you for using My Zappi.");
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withRequest(initIntentRequest())
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }

    private Request initIntentRequest() {
        return IntentRequest.builder().withLocale("en-GB").build();
    }

    private static Stream<Arguments> quitIntents() {
        return Stream.of(
                Arguments.of("AMAZON.StopIntent"),
                Arguments.of("AMAZON.NoIntent"),
                Arguments.of("AMAZON.CancelIntent"),
                Arguments.of("AMAZON.NavigateHomeIntent"));
    }
}
