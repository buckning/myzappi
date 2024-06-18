package com.amcglynn.myzappi;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Device;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.User;
import com.amazon.ask.model.interfaces.system.SystemState;

public class TestObjectCreator {
    public static HandlerInput.Builder handlerInputBuilder() {
        return HandlerInput.builder()
                .withRequestEnvelope(
                        requestEnvelopeBuilder(
                                getIntentRequestBuilder(getIntentBuilder("sample").build()).build()).build());
    }

    public static HandlerInput.Builder handlerInputBuilder(String intentName) {
        return HandlerInput.builder()
                .withRequestEnvelope(
                        requestEnvelopeBuilder(
                                getIntentRequestBuilder(getIntentBuilder(intentName).build()).build()).build());
    }

    public static RequestEnvelope.Builder requestEnvelopeBuilder(IntentRequest intentRequest) {
        return RequestEnvelope.builder()
                .withRequest(intentRequest)
                .withContext(Context.builder()
                        .withSystem(SystemState.builder().withDevice(Device.builder().withDeviceId("myDeviceId")
                                        .build())
                                .build())
                        .build())
                .withSession(Session.builder().withUser(User.builder().withUserId("test").build()).build());
    }

    public static Intent.Builder getIntentBuilder(String name) {
        return Intent.builder().withName(name);
    }

    public static IntentRequest.Builder getIntentRequestBuilder(Intent intent) {
        return IntentRequest.builder()
                .withLocale("en-GB")
                .withIntent(intent);
    }
}
