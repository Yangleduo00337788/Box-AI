package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("user_sidebar_pin")
public class UserSidebarPinDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("workspace_id")
    private Long workspaceId;
    @Column("pin_type")
    private String pinType;
    @Column("target_id")
    private Long targetId;
    @Column("sort_order")
    private Integer sortOrder;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
