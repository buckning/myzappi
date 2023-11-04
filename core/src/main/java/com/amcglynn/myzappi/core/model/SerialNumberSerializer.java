package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class SerialNumberSerializer extends JsonSerializer<SerialNumber> {

    @Override
    public void serialize(SerialNumber serialNumber, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(serialNumber.toString());
    }
}
