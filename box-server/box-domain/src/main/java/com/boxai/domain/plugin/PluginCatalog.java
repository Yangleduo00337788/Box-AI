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
    private String status;
    private Integer sortOrder;
    private Integer installCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
