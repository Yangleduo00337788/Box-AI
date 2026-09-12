package com.boxai.workspace.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateWorkspaceMemberRoleRequest(
        @NotBlank String roleCode
) {
}
