package com.amcglynn.myzappi.login.rest;

import com.amcglynn.myzappi.core.model.Tariff;
import com.amcglynn.myzappi.core.service.TariffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TariffRequestValidatorTest {

    private TariffRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TariffRequestValidator(new TariffService(null));
    }

    @MethodSource("invalidTariffs")
    @ParameterizedTest
    void testInvalidTariffs(String currency, List<Tariff> invalidTariffs) {
        var throwable = catchThrowable(() ->
                validator.validate(currency, invalidTariffs));
        assertThat(throwable).isNotNull().isInstanceOf(ServerException.class);
        assertThat(((ServerException) throwable).getStatus()).isEqualTo(400);
    }

    private static Stream<Arguments> invalidTariffs() {
        return Stream.of(
                Arguments.of("InvalidCurrency", List.of(new Tariff("24HourTariff", LocalTime.of(0, 0), LocalTime.of(0, 0), 1.0, 0.5))),
                Arguments.of("EUR", List.of(new Tariff("EndTimeBeforeStart", LocalTime.of(5, 0), LocalTime.of(2, 0), 1.0, 0.5))),
                Arguments.of("EUR", List.of(new Tariff("NameTooLong".repeat(1000), LocalTime.of(0, 0), LocalTime.of(0, 0), 1.0, 0.5))),  // name too long
                Arguments.of("EUR", List.of()),  // empty list
                Arguments.of("EUR", List.of()),  // null list
                Arguments.of("EUR", generateTariffs(25)),  // list too big
                Arguments.of("EUR", List.of(new Tariff("Test", LocalTime.of(0, 0), LocalTime.of(0, 0), 1.0, 0.5),
                        new Tariff("Test", LocalTime.of(5, 0), LocalTime.of(0, 0), 1.0, 0.5))), // overlap in tariffs
                Arguments.of("EUR", List.of(new Tariff("Test", LocalTime.of(0, 0), LocalTime.of(8, 0), 1.0, 0.5))) // incomplete tariffs. Not all times are covered
        );
    }

    private static List<Tariff> generateTariffs(int size) {
        var list = new ArrayList<Tariff>();
        IntStream.range(0, size).forEach(i -> list.add(new Tariff(String.valueOf(i), LocalTime.of(0, i), LocalTime.of(0, i + 1), 0.0, 0.0)));
        return list;
    }
}
