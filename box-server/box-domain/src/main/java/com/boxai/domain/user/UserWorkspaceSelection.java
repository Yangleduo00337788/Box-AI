package com.boxai.domain.user;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserWorkspaceSelection {

    private Long userId;
    private Long workspaceId;
    private Long selectedAgentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
