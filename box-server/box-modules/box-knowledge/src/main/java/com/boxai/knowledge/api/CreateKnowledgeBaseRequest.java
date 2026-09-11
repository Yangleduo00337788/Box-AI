package com.boxai.knowledge.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKnowledgeBaseRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 512) String description,
        @Size(max = 512) String icon
) {
}
