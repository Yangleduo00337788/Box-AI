package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("agent_knowledge")
public class AgentKnowledgeDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("agent_id")
    private Long agentId;
    @Column("version_id")
    private Long versionId;
    @Column("knowledge_base_id")
    private Long knowledgeBaseId;
    @Column("top_k")
    private Integer topK;
    @Column("score_threshold")
    private BigDecimal scoreThreshold;
    @Column("retrieval_mode")
    private String retrievalMode;
    @Column("rerank_enabled")
    private Integer rerankEnabled;
    @Column("citation_enabled")
    private Integer citationEnabled;
    @Column("created_at")
    private LocalDateTime createdAt;
}
