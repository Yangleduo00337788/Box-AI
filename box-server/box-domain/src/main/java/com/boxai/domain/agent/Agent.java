package com.boxai.domain.agent;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Agent {

    private Long id;
    private Long workspaceId;
    private String name;
    private String description;
    private String avatarUrl;
    private Long sourceTemplateId;
    private String status;
    private Long publishedVersionId;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
