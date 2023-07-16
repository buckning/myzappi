package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.TariffRepository;
import com.amcglynn.myzappi.core.model.DayTariff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

    @Mock
    private TariffRepository tariffRepository;
    private TariffService service;

    @BeforeEach
    void setUp() {
        service = new TariffService(tariffRepository);
    }

    @Test
    void testReadReturnsEmptyOptionalWhenNothingInDbForUser() {
        assertThat(service.get("unknownUser")).isNotNull().isEmpty();
    }

    @Test
    void testReadReturnsOptionalOfValueFromDb() {
        var dayTariff = new DayTariff("EUR", List.of());
        when(tariffRepository.read("user")).thenReturn(Optional.of(dayTariff));
        assertThat(service.get("user")).isNotNull().contains(dayTariff);
    }

    @Test
    void testWrite() {
        var dayTariff = new DayTariff("EUR", List.of());
        service.write("user", dayTariff);
        verify(tariffRepository).write("user", dayTariff);
    }
}
