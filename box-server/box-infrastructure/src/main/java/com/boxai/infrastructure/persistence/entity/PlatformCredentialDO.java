package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("platform_credential")
public class PlatformCredentialDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("provider_id")
    private Long providerId;
    @Column("credential_name")
    private String credentialName;
    @Column("encrypted_api_key")
    private String encryptedApiKey;
    private Integer status;
    @Column("last_used_at")
    private LocalDateTime lastUsedAt;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
