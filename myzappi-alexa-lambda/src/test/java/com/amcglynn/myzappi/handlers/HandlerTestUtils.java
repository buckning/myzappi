package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;

public class HandlerTestUtils {

    public static HandlerInput.Builder handlerInputBuilder(IntentRequest request) {
        return HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder(request).build());
    }

    public static RequestEnvelope.Builder requestEnvelopeBuilder(IntentRequest intentRequest) {
        return RequestEnvelope.builder()
                .withRequest(intentRequest)
                .withSession(Session.builder().withUser(User.builder().withUserId("userid").build()).build());
    }
}
