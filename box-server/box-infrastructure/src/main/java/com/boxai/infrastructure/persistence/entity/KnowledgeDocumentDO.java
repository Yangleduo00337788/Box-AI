package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("knowledge_document")
public class KnowledgeDocumentDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("knowledge_base_id")
    private Long knowledgeBaseId;
    private String name;
    @Column("file_name")
    private String fileName;
    @Column("file_type")
    private String fileType;
    @Column("mime_type")
    private String mimeType;
    @Column("file_size")
    private Long fileSize;
    @Column("storage_bucket")
    private String storageBucket;
    @Column("storage_key")
    private String storageKey;
    private String md5;
    @Column("page_count")
    private Integer pageCount;
    @Column("chunk_count")
    private Integer chunkCount;
    private String status;
    private Integer progress;
    @Column("error_message")
    private String errorMessage;
    @Column("created_by")
    private Long createdBy;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
