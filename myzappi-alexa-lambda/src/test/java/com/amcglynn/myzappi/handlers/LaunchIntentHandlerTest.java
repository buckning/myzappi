package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Application;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amazon.ask.model.ui.SimpleCard;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.config.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaunchIntentHandlerTest {

    private LaunchHandler handler;
    @Mock
    private Properties mockProperties;

    @BeforeEach
    void setUp() {
        handler = new LaunchHandler(mockProperties);
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
        assertThat(outputSpeech.getSsml()).isEqualTo("<speak>I can change your charge type and provide you energy usage. " +
                "Ask me to start charging or to switch to solar. You can also ask me for an energy summary.</speak>");
    }

    @Test
    void testHandleSpeechResponseForEddiSkill() {
        when(mockProperties.getEddiSkillId()).thenReturn("appId");
        var requestEnvelop = RequestEnvelope.builder()
                .withRequest(initIntentRequest())
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build())
                        .withApplication(Application.builder().withApplicationId("appId").build()).build()).build();
        var response = handler.handle(HandlerInput.builder().withRequestEnvelope(requestEnvelop).build());
        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech()).isInstanceOf(SsmlOutputSpeech.class);

        var outputSpeech = (SsmlOutputSpeech) response.get().getOutputSpeech();
        assertThat(outputSpeech.getSsml()).isEqualTo("<speak>I can control your water heater for you. " +
                "Ask me to boost your hot water for a specific duration or to enable or disable the water heater.</speak>");
    }

    @Test
    void testHandleSpeechResponseItalian() {
        var response = handler.handle(HandlerInput.builder().withRequestEnvelope(RequestEnvelope.builder()
                .withRequest(LaunchRequest.builder().withLocale("it-IT").build())
                .withSession(Session.builder()
                        .withApplication(Application.builder().withApplicationId("appId").build()).build())
                .build()).build());
        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech()).isInstanceOf(SsmlOutputSpeech.class);

        var outputSpeech = (SsmlOutputSpeech) response.get().getOutputSpeech();
        assertThat(outputSpeech.getSsml()).isEqualTo("<speak>Posso cambiare la modalità di carica e fornirti " +
                "informazioni sulla quantità di energia caricata. Chiedimi di cambiare la modalità di carica. Puoi anche " +
                "chiedermi un riepilogo sull'energia caricata.</speak>");
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
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build())
                        .withApplication(Application.builder().withApplicationId("appId").build()).build());
    }

    private LaunchRequest initIntentRequest() {
        return LaunchRequest.builder().withLocale("en-GB").build();
    }

    private HandlerInput.Builder handlerInputBuilder() {
        return HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build());
    }
}
