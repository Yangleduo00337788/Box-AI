package com.boxai.analytics.api;

import java.util.List;

public record PlatformAnalyticsOverviewVO(
        int periodDays,
        String usagePeriod,
        int tenantCount,
        int periodExecutionCount,
        int totalExecutionCount,
        double successRate,
        long avgLatencyMs,
        long periodTokens,
        int periodAiCalls,
        long monthTokens,
        int monthAiCalls,
        List<PlatformTopTenantVO> topTenants
) {
}
