package com.boxai.infrastructure.id;

import com.boxai.common.id.SnowflakeIdGenerator;
import com.boxai.infrastructure.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SnowflakeIdProperties.class)
public class SnowflakeIdConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdConfiguration.class);
    private static final String WORKER_SEQ_KEY = "box:id:worker-seq";

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(SnowflakeIdProperties properties,
                                                     ObjectProvider<RedisService> redisService) {
        int workerId = properties.getWorkerId();
        RedisService redis = redisService.getIfAvailable();
        if (redis != null) {
            try {
                workerId = (int) Math.floorMod(redis.increment(WORKER_SEQ_KEY), 32L);
                log.info("Allocated snowflake workerId={} via Redis", workerId);
            } catch (Exception ex) {
                log.warn("Failed to allocate snowflake workerId from Redis, fallback to {}", workerId, ex);
            }
        }
        return new SnowflakeIdGenerator(workerId, properties.getDatacenterId());
    }
}
