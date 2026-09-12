package com.boxai.analytics.api;

public record AnalyticsTrendPointVO(
        String date,
        int executionCount,
        double successRate,
        long avgLatencyMs
) {
}
