package com.amcglynn.myzappi;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Device;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.SupportedInterfaces;
import com.amazon.ask.model.User;
import com.amazon.ask.model.interfaces.system.SystemState;
import com.amcglynn.myzappi.core.service.ZappiService;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TestData {
    private IntentRequest intentRequest;
    private ZappiService mockZappiService;

    public TestData(String intentName) {
        this(Locale.UK, intentName, null, Map.of());
    }

    public TestData(String intentName, ZappiService mockZappiService) {
        this(Locale.UK, intentName, mockZappiService, Map.of());
    }

    public TestData(String intentName, ZappiService mockZappiService, Map<String, String> slots) {
        this(Locale.UK, intentName, mockZappiService, slots);
    }

    public TestData(Locale locale, String intentName, ZappiService mockZappiService, Map<String, String> slots) {
        var intentBuilder = Intent.builder().withName(intentName);
        slots.forEach((key, value) -> intentBuilder
                .putSlotsItem(key, Slot.builder().withValue(value).build()));

        intentRequest = IntentRequest.builder()
                .withLocale(locale.toLanguageTag())
                .withIntent(intentBuilder.build())
                .build();
        this.mockZappiService = mockZappiService;
    }

    private RequestEnvelope.Builder requestEnvelopeBuilder() {
        return RequestEnvelope.builder()
                .withRequest(intentRequest)
                .withContext(Context.builder()
                        .withSystem(SystemState.builder().withDevice(Device.builder().withDeviceId("myDeviceId")
                                        .build())
                                .build())
                        .build())
                .withSession(Session.builder().withUser(User.builder().withUserId("userId").build()).build());
    }

    public HandlerInput handlerInput() {
        var handlerInput = HandlerInput.builder()
                .withContext(Context.builder()
                        .withSystem(SystemState.builder()
                                .withDevice(Device.builder()
                                        .withDeviceId("myDeviceId")
                                        .withSupportedInterfaces(SupportedInterfaces.builder().build())
                                        .build())
                                .build())
                        .build())
                .withRequestEnvelope(requestEnvelopeBuilder().build()).build();
        var requestAttributes = new HashMap<String, Object>();
        requestAttributes.put("zoneId", ZoneId.of("Europe/Dublin"));
        requestAttributes.put("zappiService", mockZappiService);
        requestAttributes.put("userId", "mockUserId");
        handlerInput.getAttributesManager().setRequestAttributes(requestAttributes);
        return handlerInput;
    }
}
