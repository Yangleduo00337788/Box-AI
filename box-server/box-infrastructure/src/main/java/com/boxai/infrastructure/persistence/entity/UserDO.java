package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("sys_user")
public class UserDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String username;
    private String email;
    private String phone;
    @Column("password_hash")
    private String passwordHash;
    private String nickname;
    @Column("avatar_url")
    private String avatarUrl;
    private String bio;
    private Integer status;
    @Column("user_type")
    private String userType;
    @Column("last_login_at")
    private LocalDateTime lastLoginAt;
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
