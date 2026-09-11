package com.boxai.user.api;

public record AdminSystemConfigVO(
        String configKey,
        String configValue,
        String description
) {}
