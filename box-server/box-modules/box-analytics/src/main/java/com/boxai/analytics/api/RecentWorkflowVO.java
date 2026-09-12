package com.boxai.analytics.api;

import java.time.LocalDateTime;

public record RecentWorkflowVO(
        Long id,
        String name,
        String status,
        LocalDateTime updatedAt
) {
}
