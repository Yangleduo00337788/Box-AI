package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("user_oauth_identity")
public class UserOAuthIdentityDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("user_id")
    private Long userId;
    private String provider;
    @Column("provider_user_id")
    private String providerUserId;
    private String email;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
