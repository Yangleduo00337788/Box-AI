package com.boxai.user.api;

import jakarta.validation.constraints.NotNull;

public record SelectCurrentWorkspaceRequest(
        @NotNull Long workspaceId
) {}
