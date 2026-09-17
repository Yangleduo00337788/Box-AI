package com.boxai.user.api;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record OpsPlacementVO(
        Long id,
        String slot,
        String kind,
        String title,
        String body,
        String linkUrl,
        String linkLabel,
        String iconName,
        String iconUrl,
        String iconSvg,
        String imageUrl,
        String theme,
        boolean dismissible,
        String status,
        Integer sortOrder,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startsAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endsAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        long impressions,
        long clicks
) {
}
