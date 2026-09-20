package com.boxai.conversation.api;

import jakarta.validation.constraints.Size;

import java.util.List;

public record SendMessageRequest(
        @Size(max = 8000) String message,
        Boolean stream,
        Long platformModelId,
        String toolConfirmationToken,
        List<Long> pluginIds
) {
}
