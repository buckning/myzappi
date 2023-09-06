package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Application;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Permissions;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.User;
import com.amazon.ask.model.interfaces.system.SystemState;
import com.amazon.ask.model.ui.AskForPermissionsConsentCard;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.dal.AlexaToLwaLookUpRepository;
import com.amcglynn.myzappi.core.model.AlexaToLwaUserDetails;
import com.amcglynn.myzappi.core.service.UserIdResolver;
import com.amcglynn.myzappi.service.ReminderService;
import com.amcglynn.myzappi.service.ReminderServiceFactory;
import com.amcglynn.myzappi.service.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    @Mock
    private AlexaToLwaLookUpRepository mockAlexaToLwaLookUpRepository;
    private IntentRequest intentRequest;
    @Mock
    private UserIdResolver mockUserIdResolver;
    @Mock
    private SchedulerService mockSchedulerService;

    private SetReminderHandler handler;

    @BeforeEach
    void setUp() {
        when(mockReminderServiceFactory.newReminderService(any())).thenReturn(mockReminderService);
        when(mockUserZoneResolver.getZoneId(any())).thenReturn(ZoneId.of("Europe/Dublin"));
        when(mockReminderService.createDailyRecurringReminder(anyString(), any(), anyString(), any(), any()))
                .thenReturn("testAlertToken");
        when(mockUserIdResolverFactory.newUserIdResolver(any())).thenReturn(mockUserIdResolver);
        when(mockUserIdResolver.getUserId()).thenReturn("mockLwaUser");
        handler = new SetReminderHandler(mockReminderServiceFactory, mockUserZoneResolver, mockUserIdResolverFactory,
                mockAlexaToLwaLookUpRepository, mockSchedulerService);
        handler.setLocalDateTimeSupplier(() -> LocalDateTime.of(2023, 9, 1, 0, 0, 0));
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder().withName("SetReminder")
                        .putSlotsItem("time", Slot.builder().withValue("10:30").build())
                        .build())
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

    @ParameterizedTest
    @MethodSource("userWithoutPermissions")
    void testHandleRequestsConsentCardWhenReminderPermissionIsNotGranted(User user) {
        var result = handler.handle(handlerInputBuilder(requestEnvelopeBuilderWithoutPermissions(user)).build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>You have not yet granted permissions for me to set up " +
                "reminders. I sent a card on the Alexa app so you can quickly enable this. Once you have granted permissions, please ask me again.</speak>");
        var cardResponse = result.get().getCard();
        assertThat(cardResponse).isInstanceOf(AskForPermissionsConsentCard.class);
        var permissionsCard = (AskForPermissionsConsentCard) cardResponse;
        assertThat(permissionsCard.getPermissions()).hasSize(1).contains("alexa::alerts:reminders:skill:readwrite");
    }

    @Test
    void testHandleCreatedReminderAndSavesUserIdToLookUpTableWhenUserHasPermissions() {
        var result = handler.handle(handlerInputBuilder(requestEnvelopeBuilder()).build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Okay, I'll be sure to send you a daily reminder whenever your E.V. isn't connected.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Reminder set.");
        verify(mockReminderService).createDailyRecurringReminder("testConsentToken", LocalTime.of(10, 30),
                "Your E.V. is not connected. ",
                Locale.forLanguageTag("en-GB"), ZoneId.of("Europe/Dublin"));
        verify(mockAlexaToLwaLookUpRepository).read("mockAlexaUser");
        verify(mockAlexaToLwaLookUpRepository).write("mockAlexaUser", "mockLwaUser", "Europe/Dublin");
        verify(mockSchedulerService).schedule(LocalDateTime.of(2023, 9, 1, 10, 25), "mockAlexaUser", ZoneId.of("Europe/Dublin"));
    }

    @Test
    void testHandleCreatedReminderAndSchedulesCallbackForNextDayWhenReminderTimeIsAlreadyOverForTheCurrentDay() {
        // local time is 11AM, reminder time is 10:30 so the scheduled callback is 5 minutes before the next reminder on the next day
        handler.setLocalDateTimeSupplier(() -> LocalDateTime.of(2023, 9, 1, 11, 0, 0));
        var result = handler.handle(handlerInputBuilder(requestEnvelopeBuilder()).build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Okay, I'll be sure to send you a daily reminder whenever your E.V. isn't connected.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Reminder set.");
        verify(mockReminderService).createDailyRecurringReminder("testConsentToken", LocalTime.of(10, 30),
                "Your E.V. is not connected. ",
                Locale.forLanguageTag("en-GB"), ZoneId.of("Europe/Dublin"));
        verify(mockAlexaToLwaLookUpRepository).read("mockAlexaUser");
        verify(mockAlexaToLwaLookUpRepository).write("mockAlexaUser", "mockLwaUser", "Europe/Dublin");
        verify(mockSchedulerService).schedule(LocalDateTime.of(2023, 9, 2, 10, 25), "mockAlexaUser", ZoneId.of("Europe/Dublin"));
    }

    @Test
    void testHandleCreatedReminderDeletesOldLwaUserIdAndSavesNewLwaUserIdIfTheyHaveChanged() {
        when(mockAlexaToLwaLookUpRepository.read("mockAlexaUser"))
                .thenReturn(Optional.of(new AlexaToLwaUserDetails("mockAlexaUser", "invalidLwaUser", "Europe/London")));
        var result = handler.handle(handlerInputBuilder(requestEnvelopeBuilder()).build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Okay, I'll be sure to send you a daily reminder whenever your E.V. isn't connected.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Reminder set.");
        verify(mockReminderService).createDailyRecurringReminder("testConsentToken", LocalTime.of(10, 30),
                "Your E.V. is not connected. ",
                Locale.forLanguageTag("en-GB"), ZoneId.of("Europe/Dublin"));
        verify(mockAlexaToLwaLookUpRepository).read("mockAlexaUser");
        verify(mockAlexaToLwaLookUpRepository).delete("mockAlexaUser");
        verify(mockAlexaToLwaLookUpRepository).write("mockAlexaUser", "mockLwaUser", "Europe/Dublin");
    }

    @Test
    void testHandleCreatesReminderWhenUserLookUpAlreadyExists() {
        when(mockAlexaToLwaLookUpRepository.read("mockAlexaUser"))
                .thenReturn(Optional.of(new AlexaToLwaUserDetails("mockAlexaUser", "mockLwaUser", "Europe/Dublin")));
        var result = handler.handle(handlerInputBuilder(requestEnvelopeBuilder()).build());
        assertThat(result).isPresent();

        verifySpeechInResponse(result.get(), "<speak>Okay, I'll be sure to send you a daily reminder whenever your E.V. isn't connected.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Reminder set.");
        verify(mockReminderService).createDailyRecurringReminder("testConsentToken", LocalTime.of(10, 30),
                "Your E.V. is not connected. ",
                Locale.forLanguageTag("en-GB"), ZoneId.of("Europe/Dublin"));
        verify(mockAlexaToLwaLookUpRepository).read("mockAlexaUser");
        verify(mockAlexaToLwaLookUpRepository, never()).delete("mockAlexaUser");
        verify(mockAlexaToLwaLookUpRepository, never()).write("mockAlexaUser", "mockLwaUser", "Europe/Dublin");
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
                                .withUser(User.builder().withUserId("mockAlexaUser").withAccessToken("mockAccessToken")
                                        .withPermissions(Permissions.builder().withConsentToken("testConsentToken").build()).build())
                                .build())
                        .build())
                .withRequest(intentRequest)
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }

    private RequestEnvelope.Builder requestEnvelopeBuilderWithoutPermissions(User user) {
        return RequestEnvelope.builder()
                .withContext(Context.builder()
                        .withSystem(SystemState.builder()
                                .withApplication(Application.builder().withApplicationId("mockApplicationId").build())
                                .withUser(user)
                                .build())
                        .build())
                .withRequest(intentRequest)
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }

    public static Stream<Arguments> userWithoutPermissions() {
        return Stream.of(Arguments.of(User.builder().withUserId("mockAlexaUser").withAccessToken("mockAccessToken").build()),
                Arguments.of(User.builder().withUserId("mockAlexaUser").withAccessToken("mockAccessToken")
                        .withPermissions(Permissions.builder().withScopes(Map.of()).build()).build()));
    }
}
