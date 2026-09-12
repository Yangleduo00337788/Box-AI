package com.boxai.common.guard;

public record ResourceDependency(
        String resourceType,
        Long resourceId,
        String resourceName,
        String relation
) {
}
