package com.boxai.agent.chat;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.infrastructure.redis.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class ToolConfirmationService {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "box:tool-confirm:";

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public ToolConfirmationService(RedisService redisService, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    public record PendingConfirmation(
            Long userId,
            Long workspaceId,
            Long agentId,
            Long versionId,
            String toolKey,
            String toolName,
            Map<String, Object> arguments
    ) {
    }

    public String create(PendingConfirmation pending) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redisService.set(KEY_PREFIX + token, objectMapper.writeValueAsString(pending), TTL);
            return token;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法创建工具确认令牌");
        }
    }

    public PendingConfirmation consume(String token, Long userId, Long workspaceId, String toolKey) {
        PendingConfirmation pending = requirePending(token);
        if (!pending.userId().equals(userId) || !pending.workspaceId().equals(workspaceId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权确认该工具调用");
        }
        if (!pending.toolKey().equals(toolKey)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工具确认令牌与工具不匹配");
        }
        redisService.delete(KEY_PREFIX + token);
        return pending;
    }

    public PendingConfirmation peek(String token) {
        return requirePending(token);
    }

    private PendingConfirmation requirePending(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少工具确认令牌");
        }
        String raw = redisService.get(KEY_PREFIX + token.trim());
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工具确认已过期或无效");
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "工具确认数据无效");
        }
    }
}
