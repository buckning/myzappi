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
import com.amcglynn.lwa.LwaClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserZoneResolverTest {

    @Mock
    private LwaClient mockLwaClient;

    @Test
    void returnsDefaultOfEuropeDublinWhenZoneCanBeResolved() {
        var requestEnvelopeBuilder = RequestEnvelope.builder()
                .withRequest(initIntentRequest())
                .withContext(
                        Context.builder()
                                .withSystem(SystemState.builder()
                                        .withApiEndpoint("http://localhost")
                                        .withApiAccessToken("myAccessToken")
                                        .withDevice(Device.builder()
                                                .withDeviceId("myDeviceId")
                                                .build())
                                        .build())
                                .build())
                .withSession(Session.builder().withUser(User.builder()
                                .withUserId("userId")
                                .withAccessToken("mockAccessToken").build())
                        .build());

        when(mockLwaClient.getTimeZone("http://localhost", "myDeviceId", "myAccessToken"))
                .thenReturn(Optional.of("Europe/Dublin"));
        var zoneId = new UserZoneResolver(mockLwaClient).getZoneId(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder.build()).build());
        assertThat(zoneId)
                .hasToString("Europe/Dublin");
    }

    @Test
    void returnsDefaultOfEuropeLondonWhenZoneCannotBeResolved() {
        var requestEnvelopeBuilder = RequestEnvelope.builder()
                .withRequest(initIntentRequest())
                .withContext(
                        Context.builder()
                                .withSystem(SystemState.builder()
                                        .withApiEndpoint("http://localhost")
                                        .withApiAccessToken("myAccessToken")
                                        .withDevice(Device.builder()
                                                .withDeviceId("myDeviceId")
                                                .build())
                                        .build())
                                .build())
                .withSession(Session.builder().withUser(User.builder()
                                .withUserId("userId")
                                .withAccessToken("mockAccessToken").build())
                        .build());

        when(mockLwaClient.getTimeZone("http://localhost", "myDeviceId", "myAccessToken"))
                .thenReturn(Optional.empty());
        var zoneId = new UserZoneResolver(mockLwaClient).getZoneId(HandlerInput.builder()
                .withRequestEnvelope(requestEnvelopeBuilder.build()).build());
        assertThat(zoneId)
                .hasToString("Europe/London");
    }

    private IntentRequest initIntentRequest() {
        return IntentRequest.builder()
                .withIntent(Intent.builder().build())
                .build();
    }
}
