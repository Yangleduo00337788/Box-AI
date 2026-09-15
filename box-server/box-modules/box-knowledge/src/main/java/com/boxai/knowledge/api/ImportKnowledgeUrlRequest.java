package com.boxai.knowledge.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportKnowledgeUrlRequest(
        @NotBlank @Size(max = 1024) String url,
        @Size(max = 64) String syncCron
) {
}
