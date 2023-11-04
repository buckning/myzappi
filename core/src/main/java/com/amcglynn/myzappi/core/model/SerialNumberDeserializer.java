package com.amcglynn.myzappi.core.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class SerialNumberDeserializer extends JsonDeserializer<SerialNumber> {

    @Override
    public SerialNumber deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return SerialNumber.from(jsonParser.getText());
    }
}
