package com.boxai.domain.plugin;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WorkspacePluginInstall {

    private Long id;
    private Long workspaceId;
    private Long pluginId;
    private Long installedBy;
    private LocalDateTime installedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
