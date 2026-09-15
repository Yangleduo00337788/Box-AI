package com.boxai.domain.user;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserPreference {

    private Long userId;
    private String theme;
    private Boolean sendWithEnter;
    private Long currentWorkspaceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
