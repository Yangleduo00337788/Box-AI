package com.boxai.security.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class JsSafeLongDeserializer extends JsonDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return switch (p.currentToken()) {
            case VALUE_NUMBER_INT -> p.getLongValue();
            case VALUE_STRING -> {
                String text = p.getValueAsString();
                if (text == null || text.isBlank()) {
                    yield null;
                }
                yield Long.parseLong(text.trim());
            }
            case VALUE_NULL -> null;
            default -> throw new IOException("Cannot deserialize Long from " + p.currentToken());
        };
    }
}
