package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("user_preference")
public class UserPreferenceDO {

    @Id
    @Column("user_id")
    private Long userId;
    private String theme;
    @Column("send_with_enter")
    private Integer sendWithEnter;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
