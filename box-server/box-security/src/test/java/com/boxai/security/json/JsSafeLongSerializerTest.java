package com.boxai.security.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsSafeLongSerializerTest {

    private final ObjectMapper mapper = objectMapper();

    @Test
    void writesSafeLongAsNumber() throws Exception {
        assertEquals("5", mapper.writeValueAsString(5L));
    }

    @Test
    void writesSnowflakeAsString() throws Exception {
        long snowflake = 1_948_583_928_472_936_448L;
        assertEquals("\"1948583928472936448\"", mapper.writeValueAsString(snowflake));
        assertEquals(snowflake, mapper.readValue("\"1948583928472936448\"", Long.class));
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, new JsSafeLongSerializer());
        module.addSerializer(Long.TYPE, new JsSafeLongSerializer());
        module.addDeserializer(Long.class, new JsSafeLongDeserializer());
        module.addDeserializer(Long.TYPE, new JsSafeLongDeserializer());
        mapper.registerModule(module);
        return mapper;
    }
}
