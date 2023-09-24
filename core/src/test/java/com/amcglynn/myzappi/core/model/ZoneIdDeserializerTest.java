package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneIdDeserializerTest {

    @Mock
    private JsonParser mockJsonParser;

    @Mock
    private DeserializationContext mockDeserializationContext;

    @Test
    void deserialise() throws Exception {
        when(mockJsonParser.getText()).thenReturn("Europe/Dublin");
        var zoneId = new ZoneIdDeserializer().deserialize(mockJsonParser, mockDeserializationContext);
        assertThat(zoneId).isEqualTo(ZoneId.of("Europe/Dublin"));
    }
}
