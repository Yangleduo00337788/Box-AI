package com.boxai.conversation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MessageFeedbackRequest(
        @NotBlank
        @Pattern(regexp = "good|bad")
        String rating,
        String content
) {
}
