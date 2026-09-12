package com.boxai.knowledge.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record KnowledgeTestAnswerRequest(
        @NotBlank String query,
        @Min(1) @Max(20) Integer topK
) {
}
