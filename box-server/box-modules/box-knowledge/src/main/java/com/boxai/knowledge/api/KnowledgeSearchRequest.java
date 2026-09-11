package com.boxai.knowledge.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record KnowledgeSearchRequest(
        @NotBlank String query,
        @Min(1) @Max(20) Integer topK
) {
}
