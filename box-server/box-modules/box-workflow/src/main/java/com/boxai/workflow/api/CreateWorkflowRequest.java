package com.boxai.workflow.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkflowRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 512) String description
) {
}
