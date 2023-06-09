package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myzappi.Brand;
import com.amcglynn.myzappi.core.model.LoginState;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.handlers.responses.CardResponse;
import com.amcglynn.myzappi.handlers.responses.VoiceResponse;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class LoginHandler implements RequestHandler {

    private final LoginService loginService;
    private Map<LoginState, String> responses;

    public LoginHandler(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("RegisterCredentials"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        var result = loginService.login(getUserId(handlerInput), getSerialNumberFromIntent(handlerInput));

        var myZappiCode = result.getCreds().getCode().toString();
        var formattedCode = String.join(". ", myZappiCode.split("(?!^)"));
        var voiceResponse = VoiceResponse.get(result.getLoginState())
                .replace("{brandName}", Brand.NAME)
                .replace("{myZappiCode}", formattedCode);

        var cardResponse = CardResponse.get(result.getLoginState())
                .replace("{brandName}", Brand.NAME)
                .replace("{myZappiCode}", myZappiCode);

        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse)
                .withSimpleCard(Brand.NAME, cardResponse.replace("{brandName}", Brand.NAME))
                .build();
    }

    private String getUserId(HandlerInput handlerInput) {
        return handlerInput.getRequestEnvelope().getSession().getUser().getUserId();
    }

    private SerialNumber getSerialNumberFromIntent(HandlerInput handlerInput) {
        return SerialNumber.from(RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue("SerialNumber").get());    // Alexa will reject the request if serial number is not in the intent
    }
}
