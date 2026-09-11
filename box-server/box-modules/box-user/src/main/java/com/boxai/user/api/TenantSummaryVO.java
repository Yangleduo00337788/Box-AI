package com.boxai.user.api;

public record TenantSummaryVO(
        Long id,
        String name,
        String slug,
        String tenantType
) {}
