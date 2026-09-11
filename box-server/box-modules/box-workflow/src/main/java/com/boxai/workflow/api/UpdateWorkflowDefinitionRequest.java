package com.boxai.workflow.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateWorkflowDefinitionRequest(
        @NotBlank String definitionJson
) {
}
