package com.boxai.workspace.api;

public record PermissionVO(
        Long id,
        String permissionCode,
        String permissionName,
        String resourceType,
        String action
) {}
