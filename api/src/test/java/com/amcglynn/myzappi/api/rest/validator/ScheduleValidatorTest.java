package com.amcglynn.myzappi.api.rest.validator;

import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.service.Clock;
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
import java.time.ZoneId;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ScheduleValidatorTest {

    @Mock
    private Clock mockClock;
    private ScheduleValidator scheduleValidator;

    @BeforeEach
    void setUp() {
        this.scheduleValidator = new ScheduleValidator();
        when(mockClock.localDateTime(any())).thenReturn(LocalDateTime.of(2023, 9, 14, 22, 52));
        scheduleValidator.setClock(mockClock);
    }

    @Test
    void validateNoExceptionIsThrownWhenScheduleIsValid() {
        var schedule = getScheduleBuilder().build();
        scheduleValidator.validate(schedule);
    }

    @Test
    void validateThrows400WhenDateIsInThePast() {
        var schedule = getScheduleBuilder().startDateTime(LocalDateTime.of(2022, 1, 1, 1, 1)).build();
        var serverException = catchThrowableOfType(() -> scheduleValidator.validate(schedule), ServerException.class);
        assertThat(serverException.getStatus()).isEqualTo(400);
    }

    @Test
    void validateThrows400WhenDateIsInvalid() {
        var schedule = getScheduleBuilder().startDateTime(LocalDateTime.parse("-2023-09-15T22:37")).build();
        var serverException = catchThrowableOfType(() -> scheduleValidator.validate(schedule), ServerException.class);
        assertThat(serverException.getStatus()).isEqualTo(400);
    }

    @Test
    void validateThrows400WhenZoneIdIsNull() {
        var schedule = getScheduleBuilder().zoneId(null).build();
        var serverException = catchThrowableOfType(() -> scheduleValidator.validate(schedule), ServerException.class);
        assertThat(serverException.getStatus()).isEqualTo(400);
    }

    @Test
    void validateThrows400ErrorWhenIdIsSetInSchedule() {
        var schedule = getScheduleBuilder()
                .id("123")
                .build();
        var serverException = catchThrowableOfType(() -> scheduleValidator.validate(schedule), ServerException.class);
        assertThat(serverException.getStatus()).isEqualTo(400);
    }

    @Test
    void validateThrows400ErrorWhenDateTimeIsNotSet() {
        var schedule = getScheduleBuilder()
                .startDateTime(null)
                .build();
        var serverException = catchThrowableOfType(() -> scheduleValidator.validate(schedule), ServerException.class);
        assertThat(serverException.getStatus()).isEqualTo(400);
    }

    @MethodSource("validScheduleActionTypes")
    @ParameterizedTest
    void validateScheduleActionType(String actionType, String actionValue) {
        var schedule = getScheduleBuilder()
                .action(ScheduleAction.builder()
                        .type(actionType)
                        .value(actionValue)
                        .build())
                .build();
        var serverException = catchThrowableOfType(() -> scheduleValidator.validate(schedule), ServerException.class);
        assertThat(serverException).isNull();
    }

    @MethodSource("invalidScheduleActions")
    @ParameterizedTest
    void validateInvalidScheduleActions(String actionType, String actionValue) {
        var schedule = getScheduleBuilder()
                .action(ScheduleAction.builder()
                        .type(actionType)
                        .value(actionValue)
                        .build())
                .build();
        var serverException = catchThrowableOfType(() -> scheduleValidator.validate(schedule), ServerException.class);
        assertThat(serverException.getStatus()).isEqualTo(400);
    }

    @Test
    void validateInvalidScheduleActionTypeThrows400() {
        var schedule = getScheduleBuilder()
                .action(ScheduleAction.builder()
                        .type("invalidType")
                        .value("ECO_PLUS")
                        .build())
                .build();
        var serverException = catchThrowableOfType(() -> scheduleValidator.validate(schedule), ServerException.class);
        assertThat(serverException.getStatus()).isEqualTo(400);
    }

    private static Stream<Arguments> validScheduleActionTypes() {
        return Stream.of(
                Arguments.of("setChargeMode", "ECO_PLUS"),
                Arguments.of("setBoostKwh", "1"),
                Arguments.of("setBoostKwh", "5"),
                Arguments.of("setBoostKwh", "99"),
                Arguments.of("setBoostUntil", "22:37"),
                Arguments.of("setBoostFor", "PT1H")
        );
    }

    private static Stream<Arguments> invalidScheduleActions() {
        return Stream.of(
                Arguments.of("setChargeMode", "unknown"),
                Arguments.of("setChargeMode", null),
                Arguments.of("setBoostKwh", "5.5"),
                Arguments.of("setBoostKwh", "0"),
                Arguments.of("setBoostKwh", "100"),
                Arguments.of("setBoostKwh", "abc"),
                Arguments.of("setBoostKwh", "-1"),
                Arguments.of("setBoostKwh", null),
                Arguments.of("setBoostUntil", "-2023-09-15T22:37"),
                Arguments.of("setBoostUntil", "-22:37"),
                Arguments.of("setBoostFor", "T1H")
        );
    }

    private Schedule.ScheduleBuilder getScheduleBuilder() {
        return Schedule.builder()
                .zoneId(ZoneId.of("Europe/Dublin"))
                .startDateTime(LocalDateTime.of(2023, 9, 14, 23, 6))
                .action(ScheduleAction.builder()
                        .type("setChargeMode")
                        .value("ECO_PLUS")
                        .build());
    }
}
