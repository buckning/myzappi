package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ZoneIdSerializerTest {

    @Mock
    private SerializerProvider mockSerializerProvider;
    @Mock
    private JsonGenerator mockJsonGenerator;
    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Test
    void serialize() throws Exception {
        doNothing().when(mockJsonGenerator).writeString("Europe/Dublin");

        new ZoneIdSerializer().serialize(ZoneId.of("Europe/Dublin"), mockJsonGenerator, mockSerializerProvider);
        verify(mockJsonGenerator).writeString("Europe/Dublin");
    }
}
