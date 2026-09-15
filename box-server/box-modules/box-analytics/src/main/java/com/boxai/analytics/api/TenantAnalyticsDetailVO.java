package com.boxai.analytics.api;

import java.util.List;

public record TenantAnalyticsDetailVO(
        Long tenantId,
        String tenantName,
        int totalExecutions,
        double successRate,
        long totalTokens,
        List<ModelErrorStatVO> modelErrors
) {
}
