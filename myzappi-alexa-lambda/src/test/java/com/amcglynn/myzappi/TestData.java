package com.amcglynn.myzappi;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Application;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Device;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.SupportedInterfaces;
import com.amazon.ask.model.User;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.AlexaPresentationAplInterface;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.UserEvent;
import com.amazon.ask.model.interfaces.system.SystemState;
import com.amazon.ask.model.services.ServiceClientFactory;
import com.amcglynn.myzappi.core.service.ZappiService;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.amcglynn.myzappi.TestObjectCreator.handlerInputBuilder;

public class TestData {
    private final IntentRequest intentRequest;
    private final ZappiService mockZappiService;

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
                .withContext(getContext())
                .withSession(Session.builder()
                        .withUser(User.builder().withUserId("userId").build())
                        .withApplication(Application.builder().withApplicationId("eddiSkill").build())
                        .build());
    }

    public HandlerInput handlerInput() {
        return handlerInput((ServiceClientFactory) null);
    }

    public HandlerInput handlerInput(ServiceClientFactory serviceClientFactory) {
        var requestAttributes = new HashMap<String, Object>();
        requestAttributes.put("zoneId", ZoneId.of("Europe/Dublin"));
        requestAttributes.put("zappiService", mockZappiService);
        requestAttributes.put("userId", "mockUserId");
        return handlerInput(requestAttributes, serviceClientFactory);
    }

    public HandlerInput handlerInput(Map<String, Object> additionalRequestAttributes, ServiceClientFactory serviceClientFactory) {
        var handlerInput = HandlerInput.builder()
                .withServiceClientFactory(serviceClientFactory)
                .withContext(getContext())
                .withRequestEnvelope(requestEnvelopeBuilder().build()).build();
        var requestAttributes = new HashMap<>(additionalRequestAttributes);
        requestAttributes.put("zoneId", ZoneId.of("Europe/Dublin"));
        requestAttributes.put("zappiService", mockZappiService);
        requestAttributes.put("userId", "mockUserId");
        handlerInput.getAttributesManager().setRequestAttributes(requestAttributes);
        return handlerInput;
    }

    public HandlerInput handlerInput(UserEvent userEvent) {
        var requestAttributes = new HashMap<String, Object>();
        requestAttributes.put("zoneId", ZoneId.of("Europe/Dublin"));
        requestAttributes.put("zappiService", mockZappiService);
        requestAttributes.put("userId", "mockUserId");

        var handlerInput = handlerInputBuilder()
                .withRequestEnvelope(RequestEnvelope.builder()
                        .withContext(getContext())
                        .withRequest(userEvent)
                        .build())
                .build();
        handlerInput.getAttributesManager().setRequestAttributes(requestAttributes);

        return handlerInput;
    }

    private static Context getContext() {
        return Context.builder()
                .withSystem(SystemState.builder()
                        .withDevice(Device.builder()
                                .withDeviceId("myDeviceId")
                                .withSupportedInterfaces(SupportedInterfaces.builder()
                                        .withAlexaPresentationAPL(AlexaPresentationAplInterface.builder().build())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
