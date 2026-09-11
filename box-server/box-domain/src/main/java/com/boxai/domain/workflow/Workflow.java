package com.boxai.domain.workflow;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Workflow {

    private Long id;
    private Long workspaceId;
    private String name;
    private String description;
    private String status;
    private Long draftVersionId;
    private Long publishedVersionId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
