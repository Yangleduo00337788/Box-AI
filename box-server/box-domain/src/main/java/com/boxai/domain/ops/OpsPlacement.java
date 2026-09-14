package com.boxai.domain.ops;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OpsPlacement {

    private Long id;
    private String slot;
    private String kind;
    private String title;
    private String body;
    private String linkUrl;
    private String linkLabel;
    private String iconName;
    private String iconUrl;
    private String iconSvg;
    private String imageUrl;
    private String theme;
    private Integer dismissible;
    private String status;
    private Integer sortOrder;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
