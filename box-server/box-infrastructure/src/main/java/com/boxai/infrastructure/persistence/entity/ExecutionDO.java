package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("execution")
public class ExecutionDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("execution_no")
    private String executionNo;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("execution_type")
    private String executionType;
    @Column("agent_id")
    private Long agentId;
    @Column("agent_version_id")
    private Long agentVersionId;
    @Column("workflow_id")
    private Long workflowId;
    @Column("workflow_version_id")
    private Long workflowVersionId;
    @Column("conversation_id")
    private Long conversationId;
    @Column("user_id")
    private Long userId;
    private String status;
    @Column("input_json")
    private String inputJson;
    @Column("output_json")
    private String outputJson;
    @Column("error_code")
    private String errorCode;
    @Column("error_message")
    private String errorMessage;
    @Column("started_at")
    private LocalDateTime startedAt;
    @Column("finished_at")
    private LocalDateTime finishedAt;
    @Column("duration_ms")
    private Long durationMs;
    @Column("total_tokens")
    private Integer totalTokens;
    @Column("created_at")
    private LocalDateTime createdAt;
}
