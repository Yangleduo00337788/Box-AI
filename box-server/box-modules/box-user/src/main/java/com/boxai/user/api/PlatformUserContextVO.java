package com.boxai.user.api;

import java.util.List;

public record PlatformUserContextVO(
        Long userId,
        String email,
        String nickname,
        Long primaryTenantId,
        String primaryTenantName,
        String tenantType,
        Long planId,
        String planName,
        List<String> workspaceNames,
        Integer monthAiCalls,
        Long monthTokens,
        Double recentFailureRate
) {
}
