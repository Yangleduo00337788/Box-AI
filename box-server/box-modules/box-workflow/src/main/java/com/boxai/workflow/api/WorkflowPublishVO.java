package com.boxai.workflow.api;

import java.time.LocalDateTime;

public record WorkflowPublishVO(
        Long workflowId,
        String status,
        Long publishedVersionId,
        Integer publishedVersionNo,
        LocalDateTime publishedAt
) {
}
