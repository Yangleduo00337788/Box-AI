package com.boxai.domain.platform;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PlatformCredential {

    private Long id;
    private Long providerId;
    private String credentialName;
    private String encryptedApiKey;
    private Integer status;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
