package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Application;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Permissions;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amazon.ask.model.interfaces.system.SystemState;
import com.amazon.ask.model.ui.AskForPermissionsConsentCard;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.service.ReminderService;
import com.amcglynn.myzappi.service.ReminderServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetReminderHandlerTest {

    @Mock
    private ReminderServiceFactory mockReminderServiceFactory;
    @Mock
    private ReminderService mockReminderService;
    @Mock
    private UserZoneResolver mockUserZoneResolver;
    private IntentRequest intentRequest;

    private SetReminderHandler handler;

    @BeforeEach
    void setUp() {
        when(mockReminderServiceFactory.newReminderService(any())).thenReturn(mockReminderService);
        when(mockUserZoneResolver.getZoneId(any())).thenReturn(ZoneId.of("Europe/Dublin"));
        handler = new SetReminderHandler(mockReminderServiceFactory, mockUserZoneResolver);
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder().withName("SetReminder").build())
                .build();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder(requestEnvelopeBuilder()).build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("Unknown").build())
                .build();
        assertThat(handler.canHandle(handlerInputBuilder(requestEnvelopeBuilder()).build())).isFalse();
    }

    @Test
    void testHandleRequestsConsentCardWhenReminderPermissionIsNotGranted() {
        var result = handler.handle(handlerInputBuilder(requestEnvelopeBuilderWithoutPermissions()).build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>You have not yet granted permissions for me to set up " +
                "reminders. I sent a card on the Alexa app so you can quickly enable this. Once you have granted permissions, please ask me again.</speak>");
        var cardResponse = result.get().getCard();
        assertThat(cardResponse).isInstanceOf(AskForPermissionsConsentCard.class);
        var permissionsCard = (AskForPermissionsConsentCard) cardResponse;
        assertThat(permissionsCard.getPermissions()).hasSize(1).contains("alexa::alerts:reminders:skill:readwrite");
    }

    @Test
    void testHandleCreatedReminderWhenUserHasPermissions() {
        var result = handler.handle(handlerInputBuilder(requestEnvelopeBuilder()).build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Reminder set.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Reminder set.");
        verify(mockReminderService).createDailyRecurringReminder(LocalTime.of(23, 0), "test content",
                Locale.forLanguageTag("en-GB"), ZoneId.of("Europe/Dublin"));
    }

    private HandlerInput.Builder handlerInputBuilder(RequestEnvelope.Builder builder) {
        return HandlerInput.builder()
                .withRequestEnvelope(builder.build());
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withContext(Context.builder()
                        .withSystem(SystemState.builder()
                                .withApplication(Application.builder().withApplicationId("mockApplicationId").build())
                                .withUser(User.builder().withUserId("mockUser").withAccessToken("mockAccessToken")
                                        .withPermissions(Permissions.builder().withConsentToken("testConsentToken").build()).build())
                                .build())
                        .build())
                .withRequest(intentRequest)
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }

    private RequestEnvelope.Builder requestEnvelopeBuilderWithoutPermissions() {
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
