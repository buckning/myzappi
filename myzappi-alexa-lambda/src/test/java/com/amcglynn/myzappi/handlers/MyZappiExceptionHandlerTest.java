package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
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

class MyZappiExceptionHandlerTest {

    private LaunchRequest launchRequest;
    private Exception exception;

    private MyZappiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        exception = new Exception("Exception for unit test");
        handler = new MyZappiExceptionHandler();
        launchRequest = LaunchRequest.builder()
                .build();
    }

    @Test
    void testCanHandleReturnsTrue() {
        assertThat(handler.canHandle(handlerInputBuilder().build(), exception)).isTrue();
    }

    @Test
    void testHandleCardResponse() {
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build()).build(), exception);
        assertThat(response).isPresent();

        assertThat(response.get().getCard()).isInstanceOf(SimpleCard.class);
        var simpleCard = (SimpleCard) response.get().getCard();
        assertThat(simpleCard.getTitle()).isEqualTo(Brand.NAME);
        assertThat(simpleCard.getContent()).isEqualTo("There was an unexpected error.");
    }

    @Test
    void testHandleSpeechResponse() {
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build()).build(), exception);
        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech()).isInstanceOf(SsmlOutputSpeech.class);

        var outputSpeech = (SsmlOutputSpeech) response.get().getOutputSpeech();
        assertThat(outputSpeech.getSsml()).isEqualTo("<speak>There was an unexpected error.</speak>");
    }

    private HandlerInput.Builder handlerInputBuilder() {
        return HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build());
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withRequest(launchRequest)
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }
}
