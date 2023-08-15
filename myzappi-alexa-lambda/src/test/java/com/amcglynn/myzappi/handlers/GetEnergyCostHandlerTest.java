package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Device;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.User;
import com.amazon.ask.model.interfaces.system.SystemState;
import com.amcglynn.myenergi.ZappiDaySummary;
import com.amcglynn.myenergi.apiresponse.ZappiHistory;
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.TariffNotFoundException;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.model.DayCost;
import com.amcglynn.myzappi.core.model.DayTariff;
import com.amcglynn.myzappi.core.model.Tariff;
import com.amcglynn.myzappi.core.service.EnergyCostHourSummary;
import com.amcglynn.myzappi.core.service.TariffService;
import com.amcglynn.myzappi.core.service.UserIdResolver;
import com.amcglynn.myzappi.core.service.ZappiService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    private ZappiService.Builder mockZappiServiceBuilder;
    @Mock
    private ZappiService mockZappiService;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    @Mock
    private UserZoneResolver mockUserZoneResolver;
    @Mock
    private TariffService mockTariffService;
    @Mock
    private UserIdResolver mockUserIdResolver;
    private IntentRequest intentRequest;

    private GetEnergyCostHandler handler;

    @BeforeEach
    void setUp() {
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockZappiService);
        when(mockUserZoneResolver.getZoneId(any())).thenReturn(ZoneId.of("Europe/Dublin"));
        handler = new GetEnergyCostHandler(mockZappiServiceBuilder, mockUserIdResolverFactory, mockUserZoneResolver, mockTariffService);
        when(mockUserIdResolverFactory.newUserIdResolver(any())).thenReturn(mockUserIdResolver);
        when(mockUserIdResolver.getUserId()).thenReturn("mockUserId");
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("GetEnergyCost").build())
                .build();
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("SetChargeMode").build())
                .build();
        assertThat(handler.canHandle(handlerInputBuilder().build())).isFalse();
    }

    @Test
    void testHandleLocalDateThrowsTariffNotFoundExceptionWhenTariffIsNotConfigured() {
        initIntentRequest(LocalDate.of(2023, 2, 20));
        var zappiDaySummary = mock(ZappiDaySummary.class);
        when(zappiDaySummary.getSampleSize()).thenReturn(1440);
        when(zappiDaySummary.getImported()).thenReturn(new KiloWattHour(7));
        when(zappiDaySummary.getExported()).thenReturn(new KiloWattHour(10));
        when(zappiDaySummary.getConsumed()).thenReturn(new KiloWattHour(8));
        when(zappiDaySummary.getSolarGeneration()).thenReturn(new KiloWattHour(5));
        when(zappiDaySummary.getEvSummary()).thenReturn(new ZappiDaySummary.EvSummary(new KiloWattHour(3),
                new KiloWattHour(4), new KiloWattHour(7)));

        when(mockTariffService.get(anyString())).thenReturn(Optional.empty());
        var throwable = catchThrowable(() -> handler.handle(handlerInputBuilder().build()));
        assertThat(throwable).isNotNull().isInstanceOf(TariffNotFoundException.class);
    }

    @Test
    void testHandleSuccessExportCredit() {
        initIntentRequest(LocalDate.of(2023, 2, 20));

        var dayCost = new DayCost("EUR");
        var tariff = new Tariff("MockTariff", LocalTime.of(0, 0), LocalTime.of(0, 0), 1, 1);
        dayCost.add(new EnergyCostHourSummary(tariff, new ZappiHistory(2023, 1, 6, 0, 0, "Monday",
                3600000L, 7200000L, 3600000L, 3600000L, 3600000L)));

        when(mockTariffService.get(anyString())).thenReturn(Optional.of(new DayTariff("EUR", List.of(tariff))));
        when(mockTariffService.calculateCostV2(any(), any(), any(), any())).thenReturn(dayCost);

        var response = handler.handle(handlerInputBuilder().build());
        verifySpeechInResponse(response.get(), "<speak>Total credit is 1 Euro and 0 cent. You imported 1 Euro " +
                "and 0 cent. You exported 2 Euro and 0 cent. Total saved 1 Euro and 0 cent.</speak>");
        verifySimpleCardInResponse(response.get(), "My Zappi", "Total cost: €-1.00\n" +
                "Import cost: €1.00\n" +
                "Export cost: €2.00\n" +
                "Solar consumed saved: €-1.00\n" +
                "Total saved: €1.00");
    }

    @Test
    void testHandleSuccess() {
        initIntentRequest(LocalDate.of(2023, 2, 20));

        var dayCost = new DayCost("EUR");
        var tariff = new Tariff("MockTariff", LocalTime.of(0, 0), LocalTime.of(0, 0), 1, 1);
        dayCost.add(new EnergyCostHourSummary(tariff, new ZappiHistory(2023, 1, 6, 0, 0, "Monday",
                3600000L, 3600000L, 3600000L, 3600000L, 7200000L)));

        when(mockTariffService.get(anyString())).thenReturn(Optional.of(new DayTariff("EUR", List.of(tariff))));
        when(mockTariffService.calculateCostV2(any(), any(), any(), any())).thenReturn(dayCost);

        var response = handler.handle(handlerInputBuilder().build());
        verifySpeechInResponse(response.get(), "<speak>Total cost is 1 Euro and 0 cent. You imported 2 Euro " +
                "and 0 cent. You exported 1 Euro and 0 cent. Total saved 1 Euro and 0 cent.</speak>");
        verifySimpleCardInResponse(response.get(), "My Zappi", "Total cost: €1.00\n" +
                "Import cost: €2.00\n" +
                "Export cost: €1.00\n" +
                "Solar consumed saved: €0.00\n" +
                "Total saved: €1.00");
    }

    @Test
    void testHandleSuccessInGbp() {
        initIntentRequest(LocalDate.of(2023, 2, 20));

        var dayCost = new DayCost("GBP");
        var tariff = new Tariff("MockTariff", LocalTime.of(0, 0), LocalTime.of(0, 0), 1, 1);
        dayCost.add(new EnergyCostHourSummary(tariff, new ZappiHistory(2023, 1, 6, 0, 0, "Monday",
                3600000L, 3600000L, 3600000L, 3600000L, 7200000L)));

        when(mockTariffService.get(anyString())).thenReturn(Optional.of(new DayTariff("EUR", List.of(tariff))));
        when(mockTariffService.calculateCostV2(any(), any(), any(), any())).thenReturn(dayCost);

        var response = handler.handle(handlerInputBuilder().build());
        verifySpeechInResponse(response.get(), "<speak>Total cost is 1 Pound and 0 pence. You imported 2 Pounds " +
                "and 0 pence. You exported 1 Pound and 0 pence. Total saved 1 Pound and 0 pence.</speak>");
        verifySimpleCardInResponse(response.get(), "My Zappi", "Total cost: £1.00\n" +
                "Import cost: £2.00\n" +
                "Export cost: £1.00\n" +
                "Solar consumed saved: £0.00\n" +
                "Total saved: £1.00");
    }

    @Test
    void testHandleRejectsTheRequestWhenTheRequestedDateIsInTheFuture() {
        initIntentRequest(LocalDate.now().plus(1, ChronoUnit.DAYS));
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>I cannot give you a cost for a time in the future.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "I cannot give you a cost for a time in the future.");
    }

    @Test
    void testHandleUsesCurrentDateIfNoDateIsSpecified() {
        when(mockTariffService.get(anyString())).thenReturn(Optional.of(new DayTariff("EUR", List.of())));
        when(mockTariffService.calculateCostV2(any(), any(), any(), any())).thenReturn(new DayCost("EUR"));
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verify(mockZappiService).getHistory(LocalDate.now(), ZoneId.of("Europe/Dublin"));
    }

    @Test
    void testHandleReturnsErrorMessageWhenASpecificDateIsNotProvided() {
        initIntentRequest("2023-06");
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Please ask me for an energy " +
                "cost for a specific day.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Please ask me for an energy " +
                "cost for a specific day.");
    }

    private HandlerInput.Builder handlerInputBuilder() {
        return HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build());
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withRequest(intentRequest)
                .withContext(Context.builder()
                        .withSystem(SystemState.builder().withDevice(Device.builder().withDeviceId("myDeviceId")
                                        .build())
                                .build())
                        .build())
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }

    private void initIntentRequest(Object object) {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder()
                        .putSlotsItem("date", Slot.builder().withValue(object.toString()).build())
                        .withName("date").build())
                .build();
    }
}

