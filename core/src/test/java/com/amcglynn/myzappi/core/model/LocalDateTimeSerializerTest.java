package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeSerializerTest {

    @Mock
    private SerializerProvider mockSerializerProvider;
    @Mock
    private JsonGenerator mockJsonGenerator;
    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Test
    void serialize() throws Exception {
        doNothing().when(mockJsonGenerator).writeString(anyString());

        new LocalDateTimeSerializer().serialize(LocalDateTime.of(2023, 9, 13, 23, 4), mockJsonGenerator, mockSerializerProvider);
        verify(mockJsonGenerator).writeString("2023-09-13T23:04");
    }
}
