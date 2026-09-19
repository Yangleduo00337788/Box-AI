package com.boxai.domain.knowledge;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class KnowledgeDocument {

    private Long id;
    private Long workspaceId;
    private Long knowledgeBaseId;
    private String name;
    private String fileName;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private String storageBucket;
    private String storageKey;
    private String md5;
    private Integer pageCount;
    private Integer chunkCount;
    private String status;
    private Integer progress;
    private String errorMessage;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
