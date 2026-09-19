package com.boxai.user.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateOpsPlacementRequest(
        @Size(max = 1) String audience,
        @Size(max = 32) String slot,
        @Size(max = 32) String kind,
        @Size(max = 128) String title,
        @Size(max = 1024) String body,
        @Size(max = 512) String linkUrl,
        @Size(max = 64) String linkLabel,
        @Size(max = 64) String iconName,
        @Size(max = 512) String iconUrl,
        @Size(max = 65536) String iconSvg,
        @Size(max = 512) String imageUrl,
        @Size(max = 16) String theme,
        Boolean dismissible,
        String status,
        Integer sortOrder,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startsAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endsAt
) {
}
