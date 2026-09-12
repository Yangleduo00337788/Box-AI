package com.boxai.agent.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AgentChatHistoryItem(
        @NotBlank @Pattern(regexp = "USER|ASSISTANT", flags = Pattern.Flag.CASE_INSENSITIVE) String role,
        @NotBlank @Size(max = 8000) String content
) {
}
