package com.boxai.domain.notification;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Notification {

    private Long id;
    private Long workspaceId;
    private Long userId;
    private String title;
    private String content;
    private String category;
    private String linkUrl;
    private Boolean read;
    private LocalDateTime createdAt;
}
