package com.boxai.domain.publish;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Publish {

    private Long id;
    private Long workspaceId;
    private String resourceType;
    private Long resourceId;
    private Long versionId;
    private String channel;
    private String status;
    private Long publishedBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
