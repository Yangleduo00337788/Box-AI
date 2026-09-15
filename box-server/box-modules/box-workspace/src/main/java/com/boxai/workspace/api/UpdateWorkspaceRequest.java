package com.boxai.workspace.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateWorkspaceRequest(
        @NotBlank String name,
        String description,
        String avatarUrl,
        Integer status
) {}
