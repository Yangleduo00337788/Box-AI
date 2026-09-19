package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("platform_admin_inbox_dismiss")
public class PlatformAdminInboxDismissDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("admin_user_id")
    private Long adminUserId;
    @Column("notice_key")
    private String noticeKey;
    @Column("created_at")
    private LocalDateTime createdAt;
}
