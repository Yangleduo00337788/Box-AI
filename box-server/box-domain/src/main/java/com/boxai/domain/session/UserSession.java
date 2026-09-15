package com.boxai.domain.session;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserSession {

    private Long id;
    private Long userId;
    private String sessionId;
    private String deviceName;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime lastActiveAt;
    private LocalDateTime expiresAt;
    private Integer revoked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
