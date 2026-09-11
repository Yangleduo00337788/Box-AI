package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("model_credential")
public class ModelCredentialDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("provider_id")
    private Long providerId;
    @Column("credential_name")
    private String credentialName;
    @Column("encrypted_api_key")
    private String encryptedApiKey;
    @Column("encrypted_secret")
    private String encryptedSecret;
    private Integer status;
    @Column("last_used_at")
    private LocalDateTime lastUsedAt;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("created_by")
    private Long createdBy;
    @Column("updated_by")
    private Long updatedBy;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
