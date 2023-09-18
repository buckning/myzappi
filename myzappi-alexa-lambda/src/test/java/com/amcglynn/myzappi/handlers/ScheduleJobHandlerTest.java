package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.User;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.Clock;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.core.service.UserIdResolver;
import com.amcglynn.myzappi.exception.InvalidScheduleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleJobHandlerTest {

    @Mock
    private ScheduleService mockScheduleService;
    @Mock
    private UserZoneResolver mockUserZoneResolver;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    @Mock
    private UserIdResolver mockUserIdResover;
    @Mock
    private Clock mockClock;
    @Captor
    private ArgumentCaptor<Schedule> scheduleCaptor;
    private IntentRequest intentRequest;

    private ScheduleJobHandler handler;

    @BeforeEach
    void setUp() {
        when(mockUserIdResolverFactory.newUserIdResolver(any())).thenReturn(mockUserIdResover);
        when(mockUserIdResover.getUserId()).thenReturn("mockUserId");
        when(mockUserZoneResolver.getZoneId(any())).thenReturn(ZoneId.of("Europe/Dublin"));
        when(mockClock.localDateTime(any())).thenReturn(LocalDateTime.parse("2023-09-17T23:19:00"));
        when(mockClock.localDate(any())).thenReturn(LocalDate.of(2023, 9, 17));
        handler = new ScheduleJobHandler(mockScheduleService, mockUserIdResolverFactory, mockUserZoneResolver, mockClock);
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("ScheduleJob").build())
                .build();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("GoGreen").build())
                .build();
        assertThat(handler.canHandle(handlerInputBuilder().build())).isFalse();
    }

    @Test
    void testScheduleChargeModeForTimeInTheNextDay() {
        initIntentRequest();
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verify(mockScheduleService).createSchedule(eq(UserId.from("mockUserId")), scheduleCaptor.capture());

        var schedule = scheduleCaptor.getValue();
        assertThat(schedule.getStartDateTime()).isEqualTo(LocalDateTime.of(2023, 9, 18, 9, 15));
        assertThat(schedule.getAction().getType()).isEqualTo("setChargeMode");
        assertThat(schedule.getAction().getValue()).isEqualTo("ECO_PLUS");
        verifySpeechInResponse(result.get(), "<speak>Okay, I've scheduled that for you.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Created schedule.");
    }

    @Test
    void testScheduleChargeModeForTimeInTheCurrentDay() {
        when(mockClock.localDateTime(any())).thenReturn(LocalDateTime.parse("2023-09-19T05:00:00"));
        when(mockClock.localDate(any())).thenReturn(LocalDate.of(2023, 9, 19));

        initIntentRequest();
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verify(mockScheduleService).createSchedule(eq(UserId.from("mockUserId")), scheduleCaptor.capture());

        var schedule = scheduleCaptor.getValue();
        assertThat(schedule.getStartDateTime()).isEqualTo(LocalDateTime.of(2023, 9, 19, 9, 15));
        assertThat(schedule.getAction().getType()).isEqualTo("setChargeMode");
        assertThat(schedule.getAction().getValue()).isEqualTo("ECO_PLUS");
        verifySpeechInResponse(result.get(), "<speak>Okay, I've scheduled that for you.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Created schedule.");
    }

    @Test
    void testScheduleKwhBoost() {
        initIntentRequest("boostKwh", "5.0");
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verify(mockScheduleService).createSchedule(eq(UserId.from("mockUserId")), scheduleCaptor.capture());

        var schedule = scheduleCaptor.getValue();
        assertThat(schedule.getStartDateTime()).isEqualTo(LocalDateTime.of(2023, 9, 18, 9, 15));

        assertThat(schedule.getAction().getType()).isEqualTo("setBoostKwh");
        assertThat(schedule.getAction().getValue()).isEqualTo("5.0");
        verifySpeechInResponse(result.get(), "<speak>Okay, I've scheduled that for you.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Created schedule.");
    }

    @Test
    void testScheduleBoostDuration() {
        initIntentRequest("boostDuration", Duration.of(3, ChronoUnit.HOURS).toString());
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verify(mockScheduleService).createSchedule(eq(UserId.from("mockUserId")), scheduleCaptor.capture());

        var schedule = scheduleCaptor.getValue();
        assertThat(schedule.getStartDateTime()).isEqualTo(LocalDateTime.of(2023, 9, 18, 9, 15));

        assertThat(schedule.getAction().getType()).isEqualTo("setBoostFor");
        assertThat(schedule.getAction().getValue()).isEqualTo("PT3H");
        verifySpeechInResponse(result.get(), "<speak>Okay, I've scheduled that for you.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Created schedule.");
    }

    @Test
    void testScheduleBoostEndTime() {
        initIntentRequest("boostEndTime", LocalTime.of(4, 15).toString());
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verify(mockScheduleService).createSchedule(eq(UserId.from("mockUserId")), scheduleCaptor.capture());

        var schedule = scheduleCaptor.getValue();
        assertThat(schedule.getStartDateTime()).isEqualTo(LocalDateTime.of(2023, 9, 18, 9, 15));

        assertThat(schedule.getAction().getType()).isEqualTo("setBoostUntil");
        assertThat(schedule.getAction().getValue()).isEqualTo("04:15");
        verifySpeechInResponse(result.get(), "<speak>Okay, I've scheduled that for you.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Created schedule.");
    }

    @Test
    void testScheduleWithInvalidDataThrowsInvalidScheduleException() {
        initIntentRequest("invalidData", LocalTime.of(4, 15).toString());
        var throwable = catchThrowableOfType(() -> handler.handle(handlerInputBuilder().build()), InvalidScheduleException.class);
        assertThat(throwable).isNotNull();
    }

    private HandlerInput.Builder handlerInputBuilder() {
        return HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build());
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withRequest(intentRequest)
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }

    private void initIntentRequest() {
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder()
                        .putSlotsItem("scheduleTime", Slot.builder().withValue("09:15:00").build())
                        .putSlotsItem("chargeMode", Slot.builder().withValue("ECO PLUS").build())

                        .build())
                .build();
    }

    private void initIntentRequest(String slotName, String slotValue) {
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder()
                        .putSlotsItem("scheduleTime", Slot.builder().withValue("09:15:00").build())
                        .putSlotsItem(slotName, Slot.builder().withValue(slotValue).build())

                        .build())
                .build();
    }
}
