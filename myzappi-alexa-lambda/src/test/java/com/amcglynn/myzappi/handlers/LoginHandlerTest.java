package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.ui.SimpleCard;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amcglynn.myzappi.core.model.LoginResponse;
import com.amcglynn.myzappi.core.model.LoginState;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.ZappiCredentials;
import com.amcglynn.myzappi.core.model.LoginCode;
import com.amcglynn.myzappi.core.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static com.amcglynn.myzappi.handlers.HandlerTestUtils.handlerInputBuilder;
import static com.amcglynn.myzappi.handlers.HandlerTestUtils.requestEnvelopeBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginHandlerTest {

    private LoginHandler handler;
    private IntentRequest intentRequest;

    @Mock
    private LoginService mockLoginService;
    private final SerialNumber serialNumber = SerialNumber.from("12345678");
    private final LoginCode loginCode = LoginCode.from("abc123");
    private final String userId = "userid";
    private ZappiCredentials zappiCredentials;

    @BeforeEach
    void setUp() {
        zappiCredentials = new ZappiCredentials(userId, serialNumber, loginCode);
        handler = new LoginHandler(mockLoginService);
        when(mockLoginService.login(userId, serialNumber)).thenReturn(new LoginResponse(zappiCredentials, LoginState.NEW));
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder()
                        .putSlotsItem("SerialNumber", Slot.builder().withValue("12345678").build())
                        .withName("RegisterCredentials").build())
                .build();
    }

    @Test
    void testCanHandleReturnsTrueForCorrectIntent() {
        assertThat(handler.canHandle(handlerInputBuilder(intentRequest).build())).isTrue();
    }

    @Test
    void testCanHandleReturnsFalseForIncorrectIntent() {
        intentRequest = IntentRequest.builder()
                .withIntent(Intent.builder().withName("Unknown").build())
                .build();
        assertThat(handler.canHandle(handlerInputBuilder(intentRequest).build())).isFalse();
    }

    @Test
    void testHandleSpeechResponse() {
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder(intentRequest).build()).build());
        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech()).isInstanceOf(SsmlOutputSpeech.class);

        var outputSpeech = (SsmlOutputSpeech) response.get().getOutputSpeech();
        assertThat(outputSpeech.getSsml())
                .isEqualTo("<speak>Thank you, your My Zappi code is a. b. c. 1. 2. 3. " +
                        "Please use this on the My Zappi website when configuring your API key</speak>");
    }

    @Test
    void testHandleSpeechResponseReturnsCodeFromDbWhenOneAlreadyExists() {
        when(mockLoginService.login(userId, serialNumber)).thenReturn(new LoginResponse(zappiCredentials, LoginState.EXISTING_LOGIN_CODE));
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder(intentRequest).build()).build());
        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech()).isInstanceOf(SsmlOutputSpeech.class);

        var outputSpeech = (SsmlOutputSpeech) response.get().getOutputSpeech();
        assertThat(outputSpeech.getSsml())
                .isEqualTo("<speak>Thank you, your My Zappi code is a. b. c. 1. 2. 3. " +
                        "Please use this on the My Zappi website when configuring your API key</speak>");
    }

    @Test
    void testHandleSpeechResponseDoesNotReturnCodeWhenLoginWasPreviouslyCompleted() {
        when(mockLoginService.login(userId, serialNumber)).thenReturn(new LoginResponse(zappiCredentials, LoginState.LOGIN_COMPLETE));
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder(intentRequest).build()).build());
        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech()).isInstanceOf(SsmlOutputSpeech.class);

        var outputSpeech = (SsmlOutputSpeech) response.get().getOutputSpeech();
        assertThat(outputSpeech.getSsml())
                .isEqualTo("<speak>You already have Zappi credentials configured. There is no need to login again.</speak>");
    }

    @Test
    void testHandleCardResponseForNewLoginCode() {
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder(intentRequest).build()).build());
        assertThat(response).isPresent();

        assertThat(response.get().getCard()).isInstanceOf(SimpleCard.class);
        var simpleCard = (SimpleCard) response.get().getCard();
        assertThat(simpleCard.getTitle()).isEqualTo("My Zappi");
        assertThat(simpleCard.getContent()).isEqualTo("Thank you, your My Zappi code is abc123. " +
                "Please use this on the My Zappi website when configuring your API key.");
    }

    @Test
    void testHandleCardResponseReturnsExistingLoginCode() {
        when(mockLoginService.login(userId, serialNumber)).thenReturn(new LoginResponse(zappiCredentials, LoginState.EXISTING_LOGIN_CODE));
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder(intentRequest).build()).build());
        assertThat(response).isPresent();

        assertThat(response.get().getCard()).isInstanceOf(SimpleCard.class);
        var simpleCard = (SimpleCard) response.get().getCard();
        assertThat(simpleCard.getTitle()).isEqualTo("My Zappi");
        assertThat(simpleCard.getContent()).isEqualTo("Thank you, your My Zappi code is abc123. " +
                "Please use this on the My Zappi website when configuring your API key.");
    }

    @Test
    void testHandleCardResponseDoesNotReturnLoginCodeWhenTheUserIsAlreadyLoginComplete() {
        when(mockLoginService.login(userId, serialNumber)).thenReturn(new LoginResponse(zappiCredentials, LoginState.LOGIN_COMPLETE));
        var response = handler.handle(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder(intentRequest).build()).build());
        assertThat(response).isPresent();

        assertThat(response.get().getCard()).isInstanceOf(SimpleCard.class);
        var simpleCard = (SimpleCard) response.get().getCard();
        assertThat(simpleCard.getTitle()).isEqualTo("My Zappi");
        assertThat(simpleCard.getContent()).isEqualTo("You already have Zappi credentials configured. There is no need to login again.");
    }
}
