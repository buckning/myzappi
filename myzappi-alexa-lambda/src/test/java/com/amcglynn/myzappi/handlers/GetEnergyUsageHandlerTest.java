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
import com.amcglynn.myenergi.units.KiloWattHour;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySimpleCardInResponse;
import static com.amcglynn.myzappi.handlers.ResponseVerifier.verifySpeechInResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetEnergyUsageHandlerTest {

    @Mock
    private ZappiService.Builder mockZappiServiceBuilder;
    @Mock
    private ZappiService mockZappiService;
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    @Mock
    private UserZoneResolver mockUserZoneResolver;
    private IntentRequest intentRequest;

    private GetEnergyUsageHandler handler;

    @BeforeEach
    void setUp() {
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockZappiService);
        when(mockUserZoneResolver.getZoneId(any())).thenReturn(ZoneId.of("Europe/Dublin"));
        handler = new GetEnergyUsageHandler(mockZappiServiceBuilder, mockUserIdResolverFactory, mockUserZoneResolver);
        intentRequest = IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(Intent.builder().withName("GetEnergyUsage").build())
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
    void testHandleLocalDate() {
        initIntentRequest(LocalDate.of(2023, 2, 20));
        var zappiDaySummary = mock(ZappiDaySummary.class);
        when(zappiDaySummary.getSampleSize()).thenReturn(1440);
        when(zappiDaySummary.getImported()).thenReturn(new KiloWattHour(7));
        when(zappiDaySummary.getExported()).thenReturn(new KiloWattHour(10));
        when(zappiDaySummary.getConsumed()).thenReturn(new KiloWattHour(8));
        when(zappiDaySummary.getSolarGeneration()).thenReturn(new KiloWattHour(5));
        when(zappiDaySummary.getEvSummary()).thenReturn(new ZappiDaySummary.EvSummary(new KiloWattHour(3),
                new KiloWattHour(4), new KiloWattHour(7)));

        when(mockZappiService.getEnergyUsage(any(LocalDate.class), any())).thenReturn(zappiDaySummary);
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Imported 7.0 kilowatt hours. Exported 10.0 kilowatt hours. " +
                "Consumed 8.0 kilowatt hours. Solar generation was 5.0 kilowatt hours. Charged 7.0 kilowatt hours to your E.V.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Imported: 7.0kWh\n" +
                "Exported: 10.0kWh\n" +
                "Consumed: 8.0kWh\n" +
                "Solar generated: 5.0kWh\n" +
                "Charged: 7.0kWh\n");
        verify(mockZappiService).getEnergyUsage(LocalDate.of(2023, 2, 20), ZoneId.of("Europe/Dublin"));
    }

    @Test
    void testHandleRejectsTheRequestWhenTheRequestedDateIsInTheFuture() {
        initIntentRequest(LocalDate.now().plus(1, ChronoUnit.DAYS));
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>I cannot give you usage data for a time in the future.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "I cannot give you usage data for a time in the future.");
    }

    @Test
    void testHandleReturnsErrorMessageWhenDateIsNotProvided() {
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Please ask me for energy " +
                "usage for a specific date.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Please ask me for energy " +
                "usage for a specific day.");
    }

    @Test
    void testHandleReturnsErrorMessageWhenASpecificDateIsNotProvided() {
        initIntentRequest("2023-06");
        var result = handler.handle(handlerInputBuilder().build());
        assertThat(result).isPresent();
        verifySpeechInResponse(result.get(), "<speak>Please ask me for energy " +
                "usage for a specific date.</speak>");
        verifySimpleCardInResponse(result.get(), "My Zappi", "Please ask me for energy " +
                "usage for a specific day.");
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
                .withLocale("en-GB")
                .withIntent(Intent.builder()
                        .putSlotsItem("date", Slot.builder().withValue(object.toString()).build())
                        .withName("date").build())
                .build();
    }
}

