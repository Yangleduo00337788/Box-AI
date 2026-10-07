package com.boxai.security.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Snowflake IDs exceed JavaScript MAX_SAFE_INTEGER; emit them as JSON strings.
 */
public class JsSafeLongSerializer extends JsonSerializer<Long> {

    static final long JS_MAX_SAFE = 9_007_199_254_740_991L;

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (value > JS_MAX_SAFE || value < -JS_MAX_SAFE) {
            gen.writeString(Long.toString(value));
        } else {
            gen.writeNumber(value);
        }
    }
}
