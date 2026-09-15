package com.boxai.tenant.api;

import java.time.LocalDateTime;

public record AdminWorkspaceVO(
        Long id,
        String name,
        String slug,
        String description,
        String avatarUrl,
        Integer status,
        Long ownerId,
        LocalDateTime createdAt
) {}
