package com.boxai.workspace.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateRoleRequest(
        @NotBlank @Size(max = 64) String roleName,
        String description,
        List<String> permissionCodes
) {}
