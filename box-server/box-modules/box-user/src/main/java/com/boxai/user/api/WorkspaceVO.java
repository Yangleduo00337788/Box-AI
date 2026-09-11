package com.boxai.user.api;

public record WorkspaceVO(
        Long id,
        String name,
        String slug,
        String roleCode
) {}
