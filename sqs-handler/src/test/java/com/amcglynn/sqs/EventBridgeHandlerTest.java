package com.amcglynn.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.lwa.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.LinkedHashMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EventBridgeHandlerTest {

    private EventBridgeHandler handler;
    @Mock
    private Context mockContext;
    @Mock
    private LwaClient mockLwaClient;
    @Mock
    private Properties mockProperties;
    @Mock
    private MyZappiScheduleHandler mockZappiScheduleHandler;
    @Mock
    private Token mockToken;

    @BeforeEach
    void setUp() {
        handler = new EventBridgeHandler(mockProperties, mockLwaClient, mockZappiScheduleHandler);
        when(mockToken.getAccessToken()).thenReturn("mockToken");
        when(mockLwaClient.getMessagingToken(any(), any())).thenReturn(mockToken);
    }

    @Test
    void eventIsIgnoredIfEmpty() {
        var input = new LinkedHashMap<String, String>();
        handler.handleRequest(input, mockContext);
        verify(mockLwaClient, never()).postSkillMessage(anyString(), anyString(), anyString(), any());
    }

    @Test
    void eventIsIgnoredIfRequiredDataIsNotPresent() {
        var input = new LinkedHashMap<String, String>();
        input.put("unknownkey", "unknownvalues");
        handler.handleRequest(input, mockContext);
        verify(mockLwaClient, never()).postSkillMessage(anyString(), anyString(), anyString(), any());
    }

    @Test
    void postSkillMessageIsDoneWhenEventIsValid() {
        var input = new LinkedHashMap<String, String>();
        input.put("type", "reminder");
        input.put("alexaBaseUrl", "https://api.eu.amazonalexa.com");
        input.put("alexaUserId", "mockAlexaUserId");
        handler.handleRequest(input, mockContext);
        verify(mockLwaClient).getMessagingToken(any(), any());
        verify(mockLwaClient).postSkillMessage(eq("https://api.eu.amazonalexa.com"), eq("mockAlexaUserId"), eq("mockToken"), any());
    }

    @Test
    void handleMyZappiSchedule() {
        var input = new LinkedHashMap<String, String>();
        input.put("type", "setChargeMode");
        input.put("scheduleId", UUID.randomUUID().toString());
        input.put("lwaUserId", "mockLwaUserId");
        handler.handleRequest(input, mockContext);
        verify(mockLwaClient, never()).getMessagingToken(any(), any());
        verify(mockLwaClient, never()).postSkillMessage(eq("https://api.eu.amazonalexa.com"), eq("mockAlexaUserId"), eq("mockToken"), any());
    }

    @Test
    void unknownSchedulesAreIgnored() {
        var input = new LinkedHashMap<String, String>();
        input.put("type", "setChargeMode");
        handler.handleRequest(input, mockContext);
        verify(mockLwaClient, never()).getMessagingToken(any(), any());
        verify(mockLwaClient, never()).postSkillMessage(eq("https://api.eu.amazonalexa.com"), eq("mockAlexaUserId"), eq("mockToken"), any());
    }
}
