package com.boxai.conversation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank @Size(max = 8000) String message,
        Boolean stream,
        Long platformModelId
) {
}
