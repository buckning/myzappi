package com.amcglynn.myzappi.api.rest.validator;

import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.service.Clock;
import lombok.AccessLevel;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class ScheduleValidator {

    @Setter(AccessLevel.PACKAGE)
    private Clock clock;

    private static final Map<String, Predicate<String>> SUPPORTED_TYPES = Map.of("setChargeMode", ScheduleValidator::isValidChargeMode,
            "setBoostKwh", ScheduleValidator::isValidChargeInteger,
            "setBoostUntil", ScheduleValidator::isValidChargeLocalTime,
            "setBoostFor", ScheduleValidator::isValidChargeDuration);

    private static boolean isValidChargeDuration(String s) {
        try {
            Duration.parse(s);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static boolean isValidChargeLocalTime(String s) {
        try {
            LocalTime.parse(s);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static boolean isValidChargeInteger(String s) {
        try {
            var value = Integer.parseInt(s);
            return value < 100 && value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public ScheduleValidator() {
        this.clock = new Clock();
    }

    public void validate(Schedule schedule) {
        rejectIfTrue(() -> schedule.getId() != null);
        rejectIfNull(schedule.getStartDateTime());
        rejectIfNull(schedule.getZoneId());
        rejectIfTrue(() -> schedule.getStartDateTime().isBefore(clock.localDateTime(schedule.getZoneId())));
        rejectIfTrue(() -> !SUPPORTED_TYPES.containsKey(schedule.getAction().getType()));
        rejectIfTrue(() -> !SUPPORTED_TYPES.get(schedule.getAction().getType()).test(schedule.getAction().getValue()));
    }

    public void validate(ScheduleAction scheduleAction) {
        rejectIfTrue(() -> !SUPPORTED_TYPES.containsKey(scheduleAction.getType()));
        rejectIfTrue(() -> !SUPPORTED_TYPES.get(scheduleAction.getType()).test(scheduleAction.getValue()));
    }

    private void rejectIfNull(Object object) {
        rejectIfTrue(() -> object == null);
    }

    private void rejectIfTrue(BooleanSupplier supplier) {
        if (supplier.getAsBoolean()) {
            throw new ServerException(400);
        }
    }

    private static boolean isValidChargeMode(String value) {
        try {
            ZappiChargeMode.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
