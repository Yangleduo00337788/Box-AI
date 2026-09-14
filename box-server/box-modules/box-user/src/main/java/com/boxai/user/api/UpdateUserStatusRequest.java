package com.boxai.user.api;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull Integer status
) {
}
