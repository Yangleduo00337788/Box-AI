package com.boxai.agent.api;

import java.time.LocalDateTime;

public record AgentVersionVO(
        Long id,
        Integer versionNo,
        String versionName,
        String status,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean currentDraft,
        boolean published
) {}
