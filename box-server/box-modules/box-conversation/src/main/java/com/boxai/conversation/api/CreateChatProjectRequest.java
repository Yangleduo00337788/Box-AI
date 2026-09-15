package com.boxai.conversation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChatProjectRequest(
        @NotBlank @Size(max = 128) String name
) {
}
