package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeDeserializerTest {

    @Mock
    private JsonParser mockJsonParser;

    @Mock
    private DeserializationContext mockDeserializationContext;

    @Test
    void deserialise() throws Exception {
        when(mockJsonParser.getText()).thenReturn("2023-09-13T22:58:00");
        var localDateTime = new LocalDateTimeDeserializer().deserialize(mockJsonParser, mockDeserializationContext);
        assertThat(localDateTime).isEqualTo(LocalDateTime.of(2023, 9, 13, 22, 58, 0));
    }
}
