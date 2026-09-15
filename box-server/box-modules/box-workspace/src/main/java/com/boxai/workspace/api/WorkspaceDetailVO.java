package com.boxai.workspace.api;

public record WorkspaceDetailVO(
        Long id,
        String name,
        String slug,
        String description,
        String avatarUrl,
        Integer status,
        String roleCode
) {}
