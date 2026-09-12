package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("notification")
public class NotificationDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("user_id")
    private Long userId;
    private String title;
    private String content;
    private String category;
    @Column("link_url")
    private String linkUrl;
    @Column("read_flag")
    private Integer readFlag;
    @Column("created_at")
    private LocalDateTime createdAt;
}
