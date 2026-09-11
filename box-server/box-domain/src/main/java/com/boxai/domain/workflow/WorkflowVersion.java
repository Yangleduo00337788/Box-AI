package com.boxai.domain.workflow;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WorkflowVersion {

    private Long id;
    private Long workflowId;
    private Long workspaceId;
    private Integer versionNo;
    private String definitionJson;
    private String status;
    private String changeLog;
    private Long createdBy;
    private LocalDateTime createdAt;
}
