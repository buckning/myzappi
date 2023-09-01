package com.amcglynn.myzappi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Test
    void name() {
        new SchedulerService().schedule(LocalDateTime.now());
    }
}
