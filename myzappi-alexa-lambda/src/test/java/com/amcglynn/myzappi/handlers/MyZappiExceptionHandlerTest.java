package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amazon.ask.model.ui.SimpleCard;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myenergi.exception.ServerCommunicationException;
import com.amcglynn.myzappi.UserNotLinkedException;
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
                Arguments.of(new UserNotLoggedInException("test"), "You are not registered. Please register on https://myzappiunofficial.com with your my energi API key and serial number."),
                Arguments.of(new UserNotLinkedException("test"), "You need to set up account linking first on Alexa for the My Zappi skill."),
                Arguments.of(new ClientException(404), "Could not authenticate with myenergi APIs. Perhaps you entered the wrong serial number or API key. Ask me to log out and log in again to reset them."),
                Arguments.of(new ServerCommunicationException(), "I couldn't communicate with myenergi servers."),
                Arguments.of(new NullPointerException("unexpectedException"), "There was an unexpected error."));
    }

    private static Stream<Arguments> exceptionVoiceSource() {
        return Stream.of(
                Arguments.of(new UserNotLoggedInException("test"), "<speak>You are not registered. Please register on my zappi unofficial dot com with your my energy API key and serial number.</speak>"),
                Arguments.of(new UserNotLinkedException("test"), "<speak>You need to set up account linking first on Alexa for the My Zappi skill.</speak>"),
                Arguments.of(new ClientException(404), "<speak>Could not authenticate with my energy APIs. Perhaps you entered the wrong serial number or API key. Ask me to log out and log in again to reset them.</speak>"),
                Arguments.of(new ServerCommunicationException(), "<speak>I couldn't communicate with my energy servers.</speak>"),
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
