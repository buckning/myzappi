package com.amcglynn.myzappi.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Clock {
    public LocalDateTime localDateTime(ZoneId zoneId) {
        return LocalDateTime.now(zoneId);
    }

    public LocalDate localDate(ZoneId zoneId) {
        return LocalDate.now(zoneId);
    }
}
