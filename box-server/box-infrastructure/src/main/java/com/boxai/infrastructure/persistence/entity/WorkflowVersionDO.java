package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("workflow_version")
public class WorkflowVersionDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workflow_id")
    private Long workflowId;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("version_no")
    private Integer versionNo;
    private String definition;
    private String status;
    @Column("change_log")
    private String changeLog;
    @Column("created_by")
    private Long createdBy;
    @Column("created_at")
    private LocalDateTime createdAt;
}
