package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Application;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amazon.ask.model.interfaces.system.SystemState;
import com.amazon.ask.model.ui.AskForPermissionsConsentCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetReminderHandlerTest {

    private IntentRequest intentRequest;

    private SetReminderHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SetReminderHandler();
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder().withName("SetReminder").build())
                .build();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("Unknown").build())
                .build();
        assertThat(handler.canHandle(handlerInputBuilder().build())).isFalse();
    }

    @Test
    void testHandleRequestsConsentCardWhenReminderPermissionIsNotGranted() {
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>You have not yet granted permissions for me to set up " +
                "reminders. I sent a card on the Alexa app so you can quickly enable this. Once you have granted permissions, please ask me again.</speak>");
        var cardResponse = result.get().getCard();
        assertThat(cardResponse).isInstanceOf(AskForPermissionsConsentCard.class);
        var permissionsCard = (AskForPermissionsConsentCard) cardResponse;
        assertThat(permissionsCard.getPermissions()).hasSize(1).contains("alexa::alerts:reminders:skill:readwrite");
    }

    private HandlerInput.Builder handlerInputBuilder() {
        return HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build());
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withContext(Context.builder()
                        .withSystem(SystemState.builder()
                                .withApplication(Application.builder().withApplicationId("mockApplicationId").build())
                                .withUser(User.builder().withUserId("mockUser").withAccessToken("mockAccessToken").build())
                                .build())
                        .build())
                .withRequest(intentRequest)
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }
}
