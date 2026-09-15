package com.boxai.domain.user;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class User {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Integer status;
    private String userType;
    private String platformAdminRole;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
