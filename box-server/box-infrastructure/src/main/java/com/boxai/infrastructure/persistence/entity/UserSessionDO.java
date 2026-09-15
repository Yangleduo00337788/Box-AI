package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("user_session")
public class UserSessionDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("session_id")
    private String sessionId;
    @Column("device_name")
    private String deviceName;
    @Column("ip_address")
    private String ipAddress;
    @Column("user_agent")
    private String userAgent;
    @Column("last_active_at")
    private LocalDateTime lastActiveAt;
    @Column("expires_at")
    private LocalDateTime expiresAt;
    private Integer revoked;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
