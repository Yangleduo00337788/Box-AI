package com.boxai.user.api;

import java.util.List;

public record PlatformUserContextVO(
        Long userId,
        String email,
        String nickname,
        String primaryTenantName,
        String tenantType,
        List<String> workspaceNames,
        Integer monthAiCalls,
        Long monthTokens,
        Double recentFailureRate
) {
}
