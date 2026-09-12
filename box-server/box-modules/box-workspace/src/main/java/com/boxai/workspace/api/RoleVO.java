package com.boxai.workspace.api;

import java.util.List;

public record RoleVO(
        Long id,
        String roleCode,
        String roleName,
        String description,
        boolean builtIn,
        List<String> permissionCodes
) {}
