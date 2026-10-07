package com.boxai.security.json;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsSafeLongCustomizer() {
        JsSafeLongSerializer serializer = new JsSafeLongSerializer();
        JsSafeLongDeserializer deserializer = new JsSafeLongDeserializer();
        return builder -> builder
                .serializerByType(Long.class, serializer)
                .serializerByType(Long.TYPE, serializer)
                .deserializerByType(Long.class, deserializer)
                .deserializerByType(Long.TYPE, deserializer);
    }
}
