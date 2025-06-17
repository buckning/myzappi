package com.amcglynn.myzappi.handlers;

import com.amcglynn.myenergi.ZappiDaySummary;
import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.TariffNotFoundException;
import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.model.DayCost;
import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.Tariff;
import com.amcglynn.myzappi.core.service.EnergyCostHourSummary;
import com.amcglynn.myzappi.core.service.TariffService;
import com.amcglynn.myzappi.core.service.UserIdResolver;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.handlers.responses.ZappiEnergyCostCardResponse;
import com.amcglynn.myzappi.handlers.responses.ZappiEnergyCostVoiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetEnergyCostHandlerTest {

    @Mock
    private ZappiService mockZappiService;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    @Mock
    private TariffService mockTariffService;
    @Mock
    private UserIdResolver mockUserIdResolver;
    private GetEnergyCostHandler handler;
    private TestData testData;

    @BeforeEach
    void setUp() {
        testData = new TestData("GetEnergyCost", mockZappiService);

        handler = new GetEnergyCostHandler(mockTariffService);
        when(mockUserIdResolverFactory.newUserIdResolver(any())).thenReturn(mockUserIdResolver);
        when(mockUserIdResolver.getUserId()).thenReturn("mockUserId");
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(testData.handlerInput())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        assertThat(handler.canHandle(new TestData("SetChargeMode", mockZappiService).handlerInput())).isFalse();
    }

    @Test
    void testHandleLocalDateThrowsTariffNotFoundExceptionWhenTariffIsNotConfigured() {
        testData = new TestData("GetEnergyCost", mockZappiService,
                Map.of("date", LocalDate.of(2023, 2, 20).toString()));
        var zappiDaySummary = mock(ZappiDaySummary.class);
        when(zappiDaySummary.getSampleSize()).thenReturn(1440);
        when(zappiDaySummary.getImported()).thenReturn(new KiloWattHour(7));
        when(zappiDaySummary.getExported()).thenReturn(new KiloWattHour(10));
        when(zappiDaySummary.getConsumed()).thenReturn(new KiloWattHour(8));
        when(zappiDaySummary.getSolarGeneration()).thenReturn(new KiloWattHour(5));
        when(zappiDaySummary.getEvSummary()).thenReturn(new ZappiDaySummary.EvSummary(new KiloWattHour(3),
                new KiloWattHour(4), new KiloWattHour(7)));

        when(mockTariffService.get(anyString())).thenReturn(Optional.empty());
        var throwable = catchThrowable(() -> handler.handle(testData.handlerInput()));
        assertThat(throwable).isNotNull().isInstanceOf(TariffNotFoundException.class);
    }

    @Test
    void testHandleSuccessExportCredit() {
        testData = new TestData("GetEnergyCost", mockZappiService,
                Map.of("date", LocalDate.of(2023, 2, 20).toString()));

        var dayCost = new DayCost("EUR");
        var tariff = new Tariff("MockTariff", LocalTime.of(0, 0), LocalTime.of(0, 0), 1, 1);
        dayCost.add(new EnergyCostHourSummary(tariff, new ZappiHistory(2023, 1, 6, 0, 0, "Monday",
                3600000L, 7200000L, 3600000L, 3600000L, 3600000L)));

        when(mockTariffService.get(anyString())).thenReturn(Optional.of(new DayTariff("EUR", List.of(tariff))));
        when(mockTariffService.calculateCost(any(), any(), any(), any())).thenReturn(dayCost);

        var response = handler.handle(testData.handlerInput());
        verifySpeechInResponse(response.get(), "<speak>Total credit is 1 Euro and 0 cent. You imported 1 Euro " +
                "and 0 cent. You exported 2 Euro and 0 cent. Total saved 1 Euro and 0 cent.</speak>");
        verifySimpleCardInResponse(response.get(), "My Zappi", """
                Total credit: €1.00
                Import cost: €1.00
                Export cost: €2.00
                Solar consumed saved: €-1.00
                Total saved: €1.00""");
    }

    @Test
    void testHandleSuccess() {
        testData = new TestData("GetEnergyCost", mockZappiService,
                Map.of("date", LocalDate.of(2023, 2, 20).toString()));

        var dayCost = new DayCost("EUR");
        var tariff = new Tariff("MockTariff", LocalTime.of(0, 0), LocalTime.of(0, 0), 1, 1);
        dayCost.add(new EnergyCostHourSummary(tariff, new ZappiHistory(2023, 1, 6, 0, 0, "Monday",
                3600000L, 3600000L, 3600000L, 3600000L, 7200000L)));

        when(mockTariffService.get(anyString())).thenReturn(Optional.of(new DayTariff("EUR", List.of(tariff))));
        when(mockTariffService.calculateCost(any(), any(), any(), any())).thenReturn(dayCost);

        var response = handler.handle(testData.handlerInput());
        verifySpeechInResponse(response.get(), "<speak>Total cost is 1 Euro and 0 cent. You imported 2 Euro " +
                "and 0 cent. You exported 1 Euro and 0 cent. Total saved 1 Euro and 0 cent.</speak>");
        verifySimpleCardInResponse(response.get(), "My Zappi", """
                Total cost: €1.00
                Import cost: €2.00
                Export cost: €1.00
                Solar consumed saved: €0.00
                Total saved: €1.00""");
    }

    @Test
    void testHandleSuccessInItalian() {
        testData = new TestData(Locale.ITALY, "GetEnergyCost", mockZappiService,
                Map.of("date", LocalDate.of(2023, 2, 20).toString()));

        var dayCost = new DayCost("EUR");
        var tariff = new Tariff("MockTariff", LocalTime.of(0, 0), LocalTime.of(0, 0), 1, 1);
        dayCost.add(new EnergyCostHourSummary(tariff, new ZappiHistory(2023, 1, 6, 0, 0, "Monday",
                3600000L, 3600000L, 3600000L, 3600000L, 7200000L)));

        when(mockTariffService.get(anyString())).thenReturn(Optional.of(new DayTariff("EUR", List.of(tariff))));
        when(mockTariffService.calculateCost(any(), any(), any(), any())).thenReturn(dayCost);

        var response = handler.handle(testData.handlerInput());
        verifySpeechInResponse(response.get(), "<speak>Il costo totale è 1 e 0. Il costo dell'energia importata è 2 e 0. " +
                "Il costo dell'energia esportata è 1 e 0. Hai risparmiato in totale  1 e 0.</speak>");
        verifySimpleCardInResponse(response.get(), "My Zappi", """
                Costo totale: €1,00
                Costo di importazione: €2,00
                Costo di esportazione: €1,00
                Energia solare consumata risparmiata: €0,00
                Totale risparmiato: €1,00""");
    }

    @Test
    void testVoiceResponseAndCardResponseAreConsistent() {
        var dayCost = mock(DayCost.class);
        when(dayCost.getImportCost()).thenReturn(2.706100155000002);
        when(dayCost.getExportCost()).thenReturn(4.4462180000000115);
        when(dayCost.getSolarSavings()).thenReturn(2.03);
        when(dayCost.getCurrency()).thenReturn("EUR");
        var response = new ZappiEnergyCostVoiceResponse(Locale.ENGLISH, dayCost);
        assertThat(response.toString()).isEqualTo("Total credit is 1 Euro and 74 cent. You imported 2 Euro and 70 cent. You exported 4 Euro and 44 cent. Total saved 6 Euro and 47 cent. ");

        var cardResponse = new ZappiEnergyCostCardResponse(Locale.ENGLISH, dayCost);
        assertThat(cardResponse.toString()).isEqualTo("""
                Total credit: €1.74
                Import cost: €2.70
                Export cost: €4.44
                Solar consumed saved: €2.03
                Total saved: €6.47""");
    }

    @Test
    void testHandleSuccessInGbp() {
        testData = new TestData("GetEnergyCost", mockZappiService,
                Map.of("date", LocalDate.of(2023, 2, 20).toString()));

        var dayCost = new DayCost("GBP");
        var tariff = new Tariff("MockTariff", LocalTime.of(0, 0), LocalTime.of(0, 0), 1, 1);
        dayCost.add(new EnergyCostHourSummary(tariff, new ZappiHistory(2023, 1, 6, 0, 0, "Monday",
                3600000L, 3600000L, 3600000L, 3600000L, 7200000L)));

        when(mockTariffService.get(anyString())).thenReturn(Optional.of(new DayTariff("EUR", List.of(tariff))));
        when(mockTariffService.calculateCost(any(), any(), any(), any())).thenReturn(dayCost);

        var response = handler.handle(testData.handlerInput());
        verifySpeechInResponse(response.get(), "<speak>Total cost is 1 Pound and 0 pence. You imported 2 Pounds " +
                "and 0 pence. You exported 1 Pound and 0 pence. Total saved 1 Pound and 0 pence.</speak>");
        verifySimpleCardInResponse(response.get(), "My Zappi", """
                Total cost: £1.00
                Import cost: £2.00
                Export cost: £1.00
                Solar consumed saved: £0.00
                Total saved: £1.00""");
    }

    @Test
    void testHandleRejectsTheRequestWhenTheRequestedDateIsInTheFuture() {
        testData = new TestData("GetEnergyCost", mockZappiService,
                Map.of("date", LocalDate.now().plusDays(1).toString()));
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>I cannot give you a cost for a time in the future.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "I cannot give you a cost for a time in the future.");
    }

    @Test
    void testHandleUsesCurrentDateIfNoDateIsSpecified() {
        when(mockTariffService.get(anyString())).thenReturn(Optional.of(new DayTariff("EUR", List.of())));
        when(mockTariffService.calculateCost(any(), any(), any(), any())).thenReturn(new DayCost("EUR"));
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verify(mockZappiService).getHistory(LocalDate.now(), ZoneId.of("Europe/Dublin"));
    }

    @Test
    void testHandleReturnsErrorMessageWhenASpecificDateIsNotProvided() {
        testData = new TestData("GetEnergyCost", mockZappiService, Map.of("date", "2023-06"));
        var result = handler.handle(testData.handlerInput());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Please ask me for an energy " +
                "cost for a specific date.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Please ask me for an energy " +
                "cost for a specific date.");
    }
}

