package com.amcglynn.myzappi.api.rest.validator;

import com.amcglynn.myenergi.EddiMode;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.api.rest.ServerException;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.service.Clock;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

@Slf4j
public class ScheduleValidator {

    @Setter(AccessLevel.PACKAGE)
    private Clock clock;

    private static final Map<String, Predicate<String>> SUPPORTED_TYPES = Map.of("setChargeMode", ScheduleValidator::isValidChargeMode,
            "setBoostKwh", ScheduleValidator::isValidChargeInteger,
            "setBoostUntil", ScheduleValidator::isValidChargeLocalTime,
            "setSmartBoost", ScheduleValidator::isValidSmartBoost,
            "setBoostFor", ScheduleValidator::isValidChargeDuration,
            "setEddiMode", ScheduleValidator::isValidEddiMode,
            "setEddiBoostFor", ScheduleValidator::isValidEddiBoostDuration,
            "setLibbiEnabled", ScheduleValidator::isValidBoolean,
            "setLibbiChargeFromGrid", ScheduleValidator::isValidBoolean,
            "setLibbiChargeTarget", ScheduleValidator::isValidTargetSoc);

    private static boolean isValidSmartBoost(String s) {
            var tokens = s.split(";");
            if (tokens.length != 2) {
                return false;
            }
            return isValidChargeInteger(tokens[0]) && isValidChargeLocalTime(tokens[1]);
    }

    private static boolean isValidTargetSoc(String s) {
        try {
            var value = Integer.parseInt(s);
            return value <= 100 && value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isValidBoolean(String s) {
        return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
    }

    private static boolean isValidEddiBoostDuration(String s) {
        try {
            parseHeater(s);
            var duration = parseDuration(s);
            return duration.toMinutes() < 100 && duration.toMinutes() > 0;
        } catch (Exception e) {
            return false;
        }
    }

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
        rejectIfTrue(() -> schedule.getStartDateTime() == null && schedule.getRecurrence() == null);
        rejectIfNull(schedule.getZoneId());
        validateNonRecurringSchedule(schedule);
        validateRecurringSchedule(schedule);
        rejectIfTrue(() -> !SUPPORTED_TYPES.containsKey(schedule.getAction().getType()));
        rejectIfTrue(() -> !SUPPORTED_TYPES.get(schedule.getAction().getType()).test(schedule.getAction().getValue()));
        rejectIfTrue(() -> schedule.getAction().getTarget().isEmpty());
    }

    private void validateRecurringSchedule(Schedule schedule) {
        var recurrence = schedule.getRecurrence();
        if (recurrence != null) {
            rejectIfNull(recurrence.getTimeOfDay());
            rejectIfTrue(() -> recurrence.getDaysOfWeek() == null || recurrence.getDaysOfWeek().isEmpty());
            rejectIfTrue(() -> recurrence.getDaysOfWeek().size() > 7);
            rejectIfTrue(() -> recurrence.getDaysOfWeek().stream().anyMatch(day -> day < 1 || day > 7));
        }
    }

    private void validateNonRecurringSchedule(Schedule schedule) {
        if (schedule.getStartDateTime() != null) {
            rejectIfTrue(() -> schedule.getStartDateTime().isBefore(clock.localDateTime(schedule.getZoneId())));
        }
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

    private static boolean isValidEddiMode(String value) {
        try {
            EddiMode.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void parseHeater(String value) {
        // parse tank value from PT45M;tank=2
        var tokens = value.split(";");

        if (tokens.length < 2) {
            return;
        }

        validateTank(tokens[1]);
    }

    private static void validateTank(String value) {
        var tankTokens = value.split("=");

        validateVarName(tankTokens[0]);

        var heater = Integer.parseInt(tankTokens[1]);

        if (heater < 1 || heater > 2) {
            log.info("Invalid heater number {}", heater);
            throw new IllegalArgumentException("Invalid heater number");
        }
    }

    private static void validateVarName(String token) {
        if (!token.equals("tank")) {
            throw new IllegalArgumentException("Invalid variable name");
        }
    }

    private static Duration parseDuration(String value) {
        var tokens = value.split(";");
        return Duration.parse(tokens[0]);
    }
}
