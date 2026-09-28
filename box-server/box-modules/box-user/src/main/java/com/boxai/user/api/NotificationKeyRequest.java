package com.boxai.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationKeyRequest(
        @NotBlank @Size(max = 128) String key
) {
}
