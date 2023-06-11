package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amazon.ask.model.ui.SimpleCard;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.exception.UserNotLoggedInException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

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

    @MethodSource("exceptionVoiceSource")
    @ParameterizedTest
    void testUserNotLoggedInExceptionVoiceMessage(Throwable throwable, String expectedVoiceResponse) {
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build()).build(), throwable);
        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech()).isInstanceOf(SsmlOutputSpeech.class);

        var outputSpeech = (SsmlOutputSpeech) response.get().getOutputSpeech();
        assertThat(outputSpeech.getSsml()).isEqualTo(expectedVoiceResponse);
    }

    @MethodSource("exceptionCardSource")
    @ParameterizedTest
    void testUserNotLoggedInExceptionCardResponse(Throwable throwable, String expectedVoiceResponse) {
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build()).build(), throwable);
        assertThat(response).isPresent();

        assertThat(response.get().getCard()).isInstanceOf(SimpleCard.class);
        var simpleCard = (SimpleCard) response.get().getCard();
        assertThat(simpleCard.getTitle()).isEqualTo(Brand.NAME);
        assertThat(simpleCard.getContent()).isEqualTo(expectedVoiceResponse);
    }

    private static Stream<Arguments> exceptionCardSource() {
        return Stream.of(
                Arguments.of(new UserNotLoggedInException("test"), "You need to login first."),
                Arguments.of(new ClientException(404), "Could not authenticate with myenergi APIs. Perhaps you entered the wrong serial number or API key. Ask me to log out and log in again to reset them."),
                Arguments.of(new NullPointerException("unexpectedException"), "There was an unexpected error."));
    }

    private static Stream<Arguments> exceptionVoiceSource() {
        return Stream.of(
                Arguments.of(new UserNotLoggedInException("test"), "<speak>You need to login first.</speak>"),
                Arguments.of(new ClientException(404), "<speak>Could not authenticate with my energy APIs. Perhaps you entered the wrong serial number or API key. Ask me to log out and log in again to reset them.</speak>"),
                Arguments.of(new NullPointerException("unexpectedException"), "<speak>There was an unexpected error.</speak>"));
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
