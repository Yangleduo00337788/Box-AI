package com.boxai.conversation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminMessageFeedbackReplyRequest(
        @NotBlank
        @Size(max = 2000)
        String reply
) {
}
