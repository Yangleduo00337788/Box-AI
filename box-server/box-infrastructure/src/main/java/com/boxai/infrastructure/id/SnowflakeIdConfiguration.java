package com.boxai.infrastructure.id;

import com.boxai.common.id.SnowflakeIdGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SnowflakeIdProperties.class)
public class SnowflakeIdConfiguration {

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(SnowflakeIdProperties properties) {
        return new SnowflakeIdGenerator(properties.getWorkerId(), properties.getDatacenterId());
    }
}
