package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Permissions;
import com.amazon.ask.model.Request;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amazon.ask.model.interfaces.system.SystemState;
import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.LockStatus;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myzappi.core.dal.AlexaToLwaLookUpRepository;
import com.amcglynn.myzappi.core.model.AlexaToLwaUserDetails;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.service.ReminderService;
import com.amcglynn.myzappi.service.ReminderServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageReceivedHandlerTest {

    @Mock
    private MyEnergiService mockMyEnergiService;
    @Mock
    private ZappiService mockZappiService;
    @Mock
    private MyEnergiService.Builder mockZappiServiceBuilder;
    @Mock
    private AlexaToLwaLookUpRepository mockUserLookUpRepository;
    @Mock
    private ReminderServiceFactory mockReminderServiceFactory;
    @Mock
    private ReminderService mockReminderService;
    @Mock
    private Request mockRequest;

    private MessageReceivedHandler handler;

    @BeforeEach
    void setUp() {
        when(mockMyEnergiService.getZappiServiceOrThrow()).thenReturn(mockZappiService);
        when(mockZappiServiceBuilder.build(any())).thenReturn(mockMyEnergiService);
        when(mockReminderServiceFactory.newReminderService(any())).thenReturn(mockReminderService);
        when(mockUserLookUpRepository.read("mockAlexaUserId"))
                .thenReturn(Optional.of(new AlexaToLwaUserDetails("mockAlexaUser", "mockLwaUserId", "Europe/Dublin")));
        handler = new MessageReceivedHandler(mockReminderServiceFactory, mockZappiServiceBuilder, mockUserLookUpRepository);
        when(mockRequest.getType()).thenReturn("Messaging.MessageReceived");
    }

    @Test
    void testCanHandleOnlyTriggersForTheIntent() {
        assertThat(handler.canHandle(handlerInputBuilder().build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseWhenNotTheCorrectIntent() {
        when(mockRequest.getType()).thenReturn("IntentRequest");
        assertThat(handler.canHandle(handlerInputBuilder().build())).isFalse();
    }

    @Test
    void testHandleDoesNotUpdateReminderWhenThereIsNoLwaUserForTheAlexaUser() {
        when(mockUserLookUpRepository.read(anyString())).thenReturn(Optional.empty());
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        25.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.PAUSED.ordinal(), EvConnectionStatus.EV_DISCONNECTED.getCode(), LockStatus.LOCKED.getCode(), "v1.2.3"))));
        handler.handle(handlerInputBuilder().build());
        verify(mockReminderService, never()).handleReminderMessage(anyString(), anyString(), anyString(), any());
    }

    @Test
    void testHandleUpdatesReminderWhenThereIsAValidLwaUserForTheAlexaUser() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 0L, 0L,
                        25.0, 0L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.PAUSED.ordinal(), EvConnectionStatus.EV_DISCONNECTED.getCode(), LockStatus.LOCKED.getCode(), "v1.2.3"))));
        handler.handle(handlerInputBuilder().build());
        verify(mockReminderService).handleReminderMessage(eq("mockConsentToken"), eq("mockAlexaUserId"), eq("Europe/Dublin"), any());
    }

    private HandlerInput.Builder handlerInputBuilder() {
        return HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder().build());
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withRequest(mockRequest)
                .withContext(Context.builder()
                        .withSystem(SystemState.builder()
                                .withUser(User.builder()
                                        .withUserId("mockAlexaUserId")
                                        .withPermissions(Permissions.builder()
                                                .withConsentToken("mockConsentToken")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }
}
