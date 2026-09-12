package com.boxai.workspace.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteWorkspaceMemberRequest(
        @NotBlank @Email String email,
        String roleCode
) {
}
