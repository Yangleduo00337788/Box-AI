package com.boxai.domain.tool;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Tool {

    private Long id;
    private Long workspaceId;
    private String name;
    private String toolKey;
    private String description;
    private String type;
    private String inputSchemaJson;
    private String outputSchemaJson;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
