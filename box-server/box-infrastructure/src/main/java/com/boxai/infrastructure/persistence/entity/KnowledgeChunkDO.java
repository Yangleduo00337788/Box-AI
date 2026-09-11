package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("knowledge_chunk")
public class KnowledgeChunkDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("knowledge_base_id")
    private Long knowledgeBaseId;
    @Column("document_id")
    private Long documentId;
    @Column("chunk_index")
    private Integer chunkIndex;
    private String content;
    @Column("token_count")
    private Integer tokenCount;
    @Column("page_number")
    private Integer pageNumber;
    private String metadata;
    @Column("es_document_id")
    private String esDocumentId;
    private String status;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
