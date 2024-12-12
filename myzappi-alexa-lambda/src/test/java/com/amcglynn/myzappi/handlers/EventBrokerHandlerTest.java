package com.amcglynn.myzappi.handlers;

import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.RenderDocumentDirective;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.UserEvent;
import com.amcglynn.myenergi.ChargeStatus;
import com.amcglynn.myenergi.EvConnectionStatus;
import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.ZappiStatusSummary;
import com.amcglynn.myenergi.apiresponse.ZappiStatus;
import com.amcglynn.myzappi.TestData;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.core.service.ZappiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static com.amcglynn.myzappi.TestObjectCreator.handlerInputBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class EventBrokerHandlerTest {
    @Mock
    private UserIdResolverFactory mockUserIdResolverFactory;
    @Mock
    private ScheduleService mockScheduleService;
    @Mock
    private ZappiService mockZappiService;
    private String userId = "userId";
    private EventBrokerHandler handler;

    @BeforeEach
    void setUp() {
        when(mockZappiService.getStatusSummary()).thenReturn(List.of(new ZappiStatusSummary(
                new ZappiStatus("12345678", 1500L, 1400L,
                        24.3, 1000L, ZappiChargeMode.ECO_PLUS.getApiValue(),
                        ChargeStatus.BOOSTING.ordinal(), EvConnectionStatus.CHARGING.getCode()))));
        when(mockUserIdResolverFactory.newUserIdResolver(any())).thenReturn(() -> userId);
        handler = new EventBrokerHandler(mockScheduleService, mockUserIdResolverFactory);
    }

    @Test
    void canHandleReturnsTrueForUserEvent() {
        var handlerInput = handlerInputBuilder()
                .withRequestEnvelope(RequestEnvelope.builder()
                        .withRequest(UserEvent.builder().build())
                        .build())
                .build();
        assertThat(handler.canHandle(handlerInput)).isTrue();
    }

    @Test
    void canHandleReturnsFalseForNonUserEvent() {
        var handlerInput = handlerInputBuilder()
                .withRequestEnvelope(RequestEnvelope.builder()
                        .withRequest(IntentRequest.builder().build())
                        .build())
                .build();
        assertThat(handler.canHandle(handlerInput)).isFalse();
    }

    @Test
    void handleSetChargeModeReturnsExpectedResponse() {
        var userEvent = UserEvent.builder()
                .withLocale("en-GB")
                .withArguments(List.of("setChargeMode", "ECO_PLUS"))
                .build();
        var handlerInput = new TestData("fakeName", mockZappiService).handlerInput(userEvent);
        var response = handler.handle(handlerInput);
        verify(mockZappiService).setChargeMode(ZappiChargeMode.ECO_PLUS);

        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech().toString())
                .contains("Changing charge mode to Eco+. This may take a few minutes.");
        assertThat(response.get().getCard().toString())
                .contains("Changing charge mode to Eco+. This may take a few minutes.");
        assertThat(response.get().getDirectives()).hasSize(1);
        assertThat(response.get().getDirectives().get(0)).isInstanceOf(RenderDocumentDirective.class);

    }

    @Test
    void handleDeleteScheduleReturnsExpectedResponse() {
        var userEvent = UserEvent.builder()
                .withArguments(List.of("deleteSchedule", "1234"))
                .build();
        var handlerInput = handlerInputBuilder()
                .withRequestEnvelope(RequestEnvelope.builder()
                        .withRequest(userEvent)
                        .build())
                .build();

        var response = handler.handle(handlerInput);
        verify(mockScheduleService).deleteSchedule(UserId.from(userId), "1234");

        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech().toString()).contains("Schedule deleted");
        assertThat(response.get().getCard().toString()).contains("Schedule deleted");
        assertThat(response.get().getDirectives()).isEmpty();
    }

    @Test
    void handleReturnsDefaultResponseForUnknownCommand() {
        var userEvent = UserEvent.builder()
                .withArguments(List.of("unknownCommand"))
                .build();
        var handlerInput = handlerInputBuilder()
                .withRequestEnvelope(RequestEnvelope.builder()
                        .withRequest(userEvent)
                        .build())
                .build();

        var response = handler.handle(handlerInput);

        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech().toString()).contains("Oops, Andrew didn't write code to handle this!");
    }

}
