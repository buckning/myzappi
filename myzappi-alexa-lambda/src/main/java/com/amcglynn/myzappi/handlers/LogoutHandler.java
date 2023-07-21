package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.LoginService;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

public class LogoutHandler implements RequestHandler {

    private LoginService loginService;

    public LogoutHandler(LoginService loginService) {
        this.loginService = loginService;
    }
    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("Logout"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        loginService.logout(handlerInput.getRequestEnvelope().getSession().getUser().getUserId());
        var responseText = "You have been logged out.";
        return handlerInput.getResponseBuilder()
                .withSpeech(responseText)
                .withSimpleCard(Brand.NAME, responseText)
                .withShouldEndSession(false)
                .build();
    }
}
