package com.boxai.workflow.api;

import java.time.LocalDateTime;

public record WorkflowVO(
        Long id,
        String name,
        String description,
        String status,
        Long draftVersionId,
        Long publishedVersionId,
        Integer draftVersionNo,
        Integer publishedVersionNo,
        String definitionJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
