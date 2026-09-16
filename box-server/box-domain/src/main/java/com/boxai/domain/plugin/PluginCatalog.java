package com.boxai.domain.plugin;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PluginCatalog {

    private Long id;
    private String pluginCode;
    private String category;
    private String title;
    private String description;
    private String manifestJson;
    private String status;
    private String reviewStatus;
    private String visibility;
    private String tenantIdsJson;
    private Integer rolloutPercent;
    private Integer sortOrder;
    private Integer installCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
