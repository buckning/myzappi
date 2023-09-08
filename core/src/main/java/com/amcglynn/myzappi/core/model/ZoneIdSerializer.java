package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.ZoneId;

public class ZoneIdSerializer extends JsonSerializer<ZoneId> {

    @Override
    public void serialize(ZoneId zoneId, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(zoneId.toString());
    }
}
