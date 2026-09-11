package com.boxai.domain.apikey;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ApiKey {

    private Long id;
    private Long workspaceId;
    private String name;
    private String keyPrefix;
    private String keyHash;
    private Integer status;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
