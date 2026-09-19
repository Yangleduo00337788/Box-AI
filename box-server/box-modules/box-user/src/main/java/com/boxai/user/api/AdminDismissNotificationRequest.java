package com.boxai.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminDismissNotificationRequest(
        @NotBlank @Size(max = 128) String key
) {
}
