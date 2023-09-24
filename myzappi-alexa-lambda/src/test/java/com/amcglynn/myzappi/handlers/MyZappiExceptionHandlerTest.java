package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amazon.ask.model.ui.LinkAccountCard;
import com.amazon.ask.model.ui.SimpleCard;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myenergi.exception.ServerCommunicationException;
import com.amcglynn.myzappi.UserNotLinkedException;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.exception.UserNotLoggedInException;
import com.amcglynn.myzappi.exception.InvalidScheduleException;
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
                .withLocale("en-GB")
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

    @Test
    void testUserNotLinkedExceptionHasLinkAccountCardInResponse() {
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build()).build(), new UserNotLinkedException("userId"));
        assertThat(response).isPresent();

        assertThat(response.get().getCard()).isInstanceOf(LinkAccountCard.class);
    }

    private static Stream<Arguments> exceptionCardSource() {
        return Stream.of(
                Arguments.of(new UserNotLoggedInException("test"), "You are not registered. Please register on https://myzappiunofficial.com with your myenergi API key and serial number."),
                Arguments.of(new ClientException(404), "Could not authenticate with myenergi APIs. Your API key may no longer be valid. Please register again on https://myzappiunofficial.com"),
                Arguments.of(new ServerCommunicationException(), "I couldn't communicate with myenergi servers."),
                Arguments.of(new NullPointerException("unexpectedException"), "There was an unexpected error."),
                Arguments.of(new InvalidScheduleException("unexpectedException"), "I didn't understand that, please try again."));
    }

    private static Stream<Arguments> exceptionVoiceSource() {
        return Stream.of(
                Arguments.of(new UserNotLoggedInException("test"), "<speak>You are not registered. Please register on my zappi unofficial dot com with your my energy API key and serial number.</speak>"),
                Arguments.of(new UserNotLinkedException("test"), "<speak>Welcome to the My Zappi skill. To be able to use the skill, you have to link it to your Amazon account. Please go to the Alexa App and sign in with your Amazon login credentials under settings. A Link Account card was delivered to your Alexa App.</speak>"),
                Arguments.of(new ClientException(404), "<speak>Could not authenticate with my energy APIs. Your API key may no longer be valid. Please register again on my zappi unofficial dot com</speak>"),
                Arguments.of(new ServerCommunicationException(), "<speak>I couldn't communicate with my energy servers.</speak>"),
                Arguments.of(new NullPointerException("unexpectedException"), "<speak>There was an unexpected error.</speak>"),
                Arguments.of(new InvalidScheduleException("unexpectedException"), "<speak>I didn't understand that, please try again.</speak>"));
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
