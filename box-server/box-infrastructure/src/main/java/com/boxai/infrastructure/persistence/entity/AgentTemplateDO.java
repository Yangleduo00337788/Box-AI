package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("agent_template")
public class AgentTemplateDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("template_code")
    private String templateCode;
    private String name;
    private String description;
    @Column("avatar_url")
    private String avatarUrl;
    private String category;
    @Column("system_prompt")
    private String systemPrompt;
    @Column("platform_model_id")
    private Long platformModelId;
    private BigDecimal temperature;
    @Column("top_p")
    private BigDecimal topP;
    @Column("max_tokens")
    private Integer maxTokens;
    @Column("stream_enabled")
    private Integer streamEnabled;
    private String status;
    @Column("review_status")
    private String reviewStatus;
    private String visibility;
    @Column("tenant_ids_json")
    private String tenantIdsJson;
    @Column("rollout_percent")
    private Integer rolloutPercent;
    @Column("sort_order")
    private Integer sortOrder;
    @Column("install_count")
    private Integer installCount;
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
