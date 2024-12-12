package com.amcglynn.myzappi.handlers;

import com.amazon.ask.model.interfaces.alexa.presentation.apl.RenderDocumentDirective;
import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.Clock;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.core.service.UserIdResolver;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.exception.InvalidScheduleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.Map;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private ZappiService mockZappiService;
    @Captor
    private ArgumentCaptor<Schedule> scheduleCaptor;

    private ScheduleJobHandler handler;
    private TestData testData;

    @BeforeEach
    void setUp() {
        when(mockUserIdResolverFactory.newUserIdResolver(any())).thenReturn(mockUserIdResover);
        when(mockUserIdResover.getUserId()).thenReturn("mockUserId");
        when(mockUserZoneResolver.getZoneId(any())).thenReturn(ZoneId.of("Europe/Dublin"));
        when(mockClock.localDateTime(any())).thenReturn(LocalDateTime.parse("2023-09-17T23:19:00"));
        when(mockClock.localDate(any())).thenReturn(LocalDate.of(2023, 9, 17));
        when(mockScheduleService.createSchedule(any(), any())).thenReturn(
                Schedule.builder()
                        .id("scheduleId")
                        .startDateTime(LocalDateTime.of(2023, 9, 18, 9, 15))
                        .action(ScheduleAction.builder()
                                .type("setBoostKwh")
                                .value("50").build())
                        .build());
        handler = new ScheduleJobHandler(mockScheduleService, mockUserIdResolverFactory, mockUserZoneResolver, mockClock);

        testData = new TestData("ScheduleJob", mockZappiService, Map.of(
                "scheduleTime", "09:15:00",
                "chargeMode", "ECO PLUS"
        ));
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(testData.handlerInput())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        assertThat(handler.canHandle(new TestData("Unknown").handlerInput())).isFalse();
    }

    @Test
    void testScheduleChargeModeForTimeInTheNextDay() {
        var result = handler.handle(testData.handlerInput());
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

        var result = handler.handle(testData.handlerInput());
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
        testData = new TestData("ScheduleJob", mockZappiService, Map.of(
                "scheduleTime", "09:15:00",
                "boostKwh", "5.0"
        ));

        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verify(mockScheduleService).createSchedule(eq(UserId.from("mockUserId")), scheduleCaptor.capture());

        var schedule = scheduleCaptor.getValue();
        assertThat(schedule.getStartDateTime()).isEqualTo(LocalDateTime.of(2023, 9, 18, 9, 15));

        assertThat(schedule.getAction().getType()).isEqualTo("setBoostKwh");
        assertThat(schedule.getAction().getValue()).isEqualTo("5.0");
        verifySpeechInResponse(result.get(), "<speak>Okay, I've scheduled that for you.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Created schedule.");
        assertThat(result.get().getDirectives()).isNotEmpty();
        assertThat(result.get().getDirectives().get(0)).isInstanceOf(RenderDocumentDirective.class);
        assertThat(((RenderDocumentDirective)result.get().getDirectives().get(0)).getDocument().toString())
                .isEqualTo("""
                        {import=[{name=alexa-layouts, version=1.7.0}], \
                        mainTemplate={items=[{items=[{wrap=wrap, paddingLeft=5vw, justifyContent=start, direction=row, \
                        grow=1, items=[{alignItems=start, justifyContent=start, items=[{type=Text, text=Schedule Details, \
                        color=grey, fontSize=30, textAlign=center, paddingBottom=20dp}, \
                        {type=Text, text=Schedule Type: <span color='white'>One-time</span>, color=grey, fontSize=20dp, \
                        textAlign=center}, {type=Text, text=Start time: <span color='white'>09:15</span>, color=grey, \
                        fontSize=20dp, textAlign=center}, {type=Text, text=Start date: <span color='white'>2023-09-18</span>, \
                        color=grey, fontSize=20dp, textAlign=center}, {type=Text, text=${payload.importing}, \
                        color=${@myenergiRed}, fontSize=40dp, textAlign=center, paddingBottom=20dp}, \
                        {type=Text, text=Schedule Action, color=grey, fontSize=30dp, textAlign=center}, \
                        {type=Text, text=Schedule Type: <span color='white'>setBoostKwh</span>, color=grey, \
                        fontSize=20dp, textAlign=center}, {type=Text, text=Value: <span color='white'>50</span>, \
                        color=grey, fontSize=20dp, textAlign=center}], type=Container, width=85vw}, {direction=row, \
                        alignItems=start, justifyContent=start, items=[{type=AlexaButton, buttonText=Delete, \
                        primaryAction={type=SendEvent, arguments=[deleteSchedule, scheduleId]}}], wrap=wrap, grow=1, \
                        position=relative, type=Container, width=100vw, paddingTop=10vh}], alignSelf=center, position=absolute, \
                        alignItems=center, type=Container, height=90vh, width=100vw}], justifyContent=center, \
                        alignItems=center, wrap=wrap, layoutDirection=LTR, type=Container}]}, resources=[{colors={myenergiRed=#FF4400, \
                        myenergiGreen=#55FF00, myenergiYellow=#FFD701, myenergiBlue=#0186FF, myenergiPurple=#FF008C}}], \
                        type=APL, version=1.8}\
                        """);
    }

    @Test
    void testScheduleBoostDuration() {
        testData = new TestData("ScheduleJob", mockZappiService, Map.of(
                "scheduleTime", "09:15:00",
                "boostDuration", Duration.of(3, ChronoUnit.HOURS).toString()
        ));

        var result = handler.handle(testData.handlerInput());
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
        testData = new TestData("ScheduleJob", mockZappiService, Map.of(
                "scheduleTime", "09:15:00",
                "boostEndTime", LocalTime.of(4, 15).toString()
        ));

        var result = handler.handle(testData.handlerInput());
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
        testData = new TestData("ScheduleJob", mockZappiService, Map.of(
                "scheduleTime", "09:15:00",
                "invalidData", LocalTime.of(4, 15).toString()
        ));

        var throwable = catchThrowableOfType(() ->  handler.handle(testData.handlerInput()), InvalidScheduleException.class);
        assertThat(throwable).isNotNull();
    }
}
