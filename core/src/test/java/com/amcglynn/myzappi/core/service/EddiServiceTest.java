package com.amcglynn.myzappi.core.service;

import com.amcglynn.myenergi.MyEnergiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EddiServiceTest {

    private EddiService eddiService;
    @Mock
    private MyEnergiClient mockClient;

    @BeforeEach
    void setUp() {
        this.eddiService = new EddiService(mockClient);
    }

    @Test
    void testBoostEddi() {
        this.eddiService = new EddiService(mockClient);

        eddiService.boostEddi(Duration.of(1, ChronoUnit.HOURS));
        verify(mockClient).boostEddi(Duration.of(1, ChronoUnit.HOURS), 1);
    }

    @Test
    void testStopEddiBoost() {
        this.eddiService = new EddiService(mockClient);
        eddiService.stopEddiBoost();
        verify(mockClient).stopEddiBoost(1);
    }

    @Test
    void testStopEddiBoostFor2ndHeater() {
        this.eddiService = new EddiService(mockClient);

        eddiService.stopEddiBoost(2);
        verify(mockClient).stopEddiBoost(2);
    }

    @Test
    void testEddiValidationThrowsIllegalArgumentExceptionWhenHeaterIsLessThan1() {
        this.eddiService = new EddiService(mockClient);

        var exception = catchThrowableOfType(() -> eddiService.stopEddiBoost(0), IllegalArgumentException.class);
        assertThat(exception).isNotNull();
        verify(mockClient, never()).stopEddiBoost(anyInt());
    }

    @Test
    void testEddiValidationThrowsIllegalArgumentExceptionWhenHeaterIsGreaterThan2() {
        this.eddiService = new EddiService(mockClient);

        var exception = catchThrowableOfType(() -> eddiService.stopEddiBoost(3), IllegalArgumentException.class);
        assertThat(exception).isNotNull();
        verify(mockClient, never()).stopEddiBoost(anyInt());
    }
}
