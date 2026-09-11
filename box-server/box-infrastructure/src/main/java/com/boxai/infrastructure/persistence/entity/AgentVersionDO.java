package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("agent_version")
public class AgentVersionDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("agent_id")
    private Long agentId;
    @Column("version_no")
    private Integer versionNo;
    @Column("version_name")
    private String versionName;
    private String status;
    @Column("system_prompt")
    private String systemPrompt;
    @Column("model_id")
    private Long modelId;
    @Column("platform_model_id")
    private Long platformModelId;
    @Column("model_source")
    private String modelSource;
    private BigDecimal temperature;
    @Column("top_p")
    private BigDecimal topP;
    @Column("max_tokens")
    private Integer maxTokens;
    @Column("stream_enabled")
    private Integer streamEnabled;
    @Column("memory_enabled")
    private Integer memoryEnabled;
    @Column("memory_window_size")
    private Integer memoryWindowSize;
    @Column("knowledge_enabled")
    private Integer knowledgeEnabled;
    @Column("tool_enabled")
    private Integer toolEnabled;
    @Column("config_json")
    private String configJson;
    @Column("published_at")
    private LocalDateTime publishedAt;
    @Column("created_by")
    private Long createdBy;
    @Column("updated_by")
    private Long updatedBy;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
