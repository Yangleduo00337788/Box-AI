package com.boxai.analytics.api;

public record ModelErrorStatVO(
        String modelName,
        int errorCount,
        int totalCount
) {
}
