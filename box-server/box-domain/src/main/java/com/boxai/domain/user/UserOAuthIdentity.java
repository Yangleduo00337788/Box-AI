package com.boxai.domain.user;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserOAuthIdentity {

    private Long id;
    private Long userId;
    private String provider;
    private String providerUserId;
    private String email;
    private LocalDateTime createdAt;
}
