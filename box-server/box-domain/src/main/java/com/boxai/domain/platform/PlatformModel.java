package com.boxai.domain.platform;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PlatformModel {

    private Long id;
    private Long providerId;
    private String modelCode;
    private String modelName;
    private String description;
    private String modelType;
    private Boolean supportStreaming;
    private Integer contextWindow;
    private Integer maxOutputTokens;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
