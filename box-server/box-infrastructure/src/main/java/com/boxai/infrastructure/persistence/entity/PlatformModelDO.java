package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("platform_model")
public class PlatformModelDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("provider_id")
    private Long providerId;
    @Column("model_code")
    private String modelCode;
    @Column("model_name")
    private String modelName;
    private String description;
    @Column("model_type")
    private String modelType;
    @Column("support_streaming")
    private Integer supportStreaming;
    @Column("context_window")
    private Integer contextWindow;
    @Column("max_output_tokens")
    private Integer maxOutputTokens;
    @Column("sort_order")
    private Integer sortOrder;
    private Integer status;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
