package com.amcglynn.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsHandlerTest {

    @Mock
    private Context mockContext;
    @Mock
    private SQSEvent mockSqsEvent;
    @Mock
    private SQSEvent.SQSMessage mockRecord;

    @Test
    void handleEvent() {
        when(mockRecord.getBody()).thenReturn("{\n\"type\": \"reminderUpdate\",\n\"alexaBaseUrl\":\"https://api.eu.amazonalexa.com\",\n\"alexaUserId\": \"testUser123\"\n}");
        when(mockSqsEvent.getRecords()).thenReturn(List.of(mockRecord));
        new SqsHandler().handleRequest(mockSqsEvent, mockContext);
    }
}
