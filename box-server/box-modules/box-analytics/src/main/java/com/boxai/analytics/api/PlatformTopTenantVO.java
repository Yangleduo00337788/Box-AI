package com.boxai.analytics.api;

public record PlatformTopTenantVO(
        Long tenantId,
        String tenantName,
        int aiCalls,
        long tokens
) {
}
