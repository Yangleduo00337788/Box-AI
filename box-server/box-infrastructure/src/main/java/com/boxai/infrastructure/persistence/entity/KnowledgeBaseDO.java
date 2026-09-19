package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("knowledge_base")
public class KnowledgeBaseDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    private String name;
    private String description;
    private String icon;
    @Column("embedding_model_id")
    private Long embeddingModelId;
    @Column("rerank_model_id")
    private Long rerankModelId;
    @Column("ocr_model_id")
    private Long ocrModelId;
    @Column("chunk_config")
    private String chunkConfig;
    @Column("retrieval_config")
    private String retrievalConfig;
    @Column("document_count")
    private Integer documentCount;
    @Column("chunk_count")
    private Long chunkCount;
    private String status;
    @Column("created_by")
    private Long createdBy;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
