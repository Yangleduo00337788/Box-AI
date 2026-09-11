package com.boxai.conversation.api;

public record SearchResultVO(
        String type,
        Long id,
        String title,
        String subtitle,
        String route
) {}
