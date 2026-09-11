package com.boxai.agent.api;

import java.time.LocalDateTime;

public record AgentPublishVO(
        Long agentId,
        String status,
        Long publishedVersionId,
        Integer publishedVersionNo,
        String publishedVersionName,
        LocalDateTime publishedAt
) {
}
