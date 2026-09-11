package com.boxai.tenant.api;

import java.time.LocalDateTime;

public record TenantVO(
        Long id,
        String name,
        String slug,
        String tenantType,
        Long planId,
        String planName,
        String contactEmail,
        Integer status,
        Long ownerId,
        LocalDateTime createdAt
) {}
