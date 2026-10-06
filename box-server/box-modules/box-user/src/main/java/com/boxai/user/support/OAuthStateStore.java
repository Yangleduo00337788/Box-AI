package com.boxai.user.support;

import com.boxai.infrastructure.redis.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class OAuthStateStore {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "box:oauth:state:";

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public OAuthStateStore(RedisService redisService, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    public String create(OAuthStatePayload payload) {
        String state = UUID.randomUUID().toString().replace("-", "");
        try {
            redisService.set(KEY_PREFIX + state, objectMapper.writeValueAsString(payload), TTL);
            return state;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("OAuth state 序列化失败", ex);
        }
    }

    public Optional<OAuthStatePayload> consume(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        String key = KEY_PREFIX + state.trim();
        String raw = redisService.get(key);
        redisService.delete(key);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, OAuthStatePayload.class));
        } catch (JsonProcessingException ex) {
            return Optional.empty();
        }
    }

    public record OAuthStatePayload(
            String provider,
            String portal,
            String redirectUri
    ) {
    }
}
